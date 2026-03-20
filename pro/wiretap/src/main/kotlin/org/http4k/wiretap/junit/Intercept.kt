/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ResponseFilters
import org.http4k.wiretap.WiretapContext
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.toDetail
import org.http4k.wiretap.domain.toSummary
import org.http4k.wiretap.junit.RenderMode.Always
import org.http4k.wiretap.junit.RenderMode.Never
import org.http4k.wiretap.junit.RenderMode.OnFailure
import org.http4k.wiretap.otel.TraceDetailView
import org.http4k.wiretap.otel.TraceDiagramView
import org.http4k.wiretap.otel.WiretapOpenTelemetry
import org.http4k.wiretap.otel.toMermaid
import org.http4k.wiretap.otel.toSequenceDiagram
import org.http4k.wiretap.otel.toTraceDetail
import org.http4k.wiretap.traffic.TransactionDetailView
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.security.SecureRandom
import java.time.Clock
import java.util.Random
import java.util.concurrent.atomic.AtomicReference

enum class RenderMode { Never, OnFailure, Always }

class Intercept @JvmOverloads constructor(
    private val httpClient: HttpHandler = JavaHttpClient(),
    private val renderMode: RenderMode = OnFailure,
    private val clock: Clock = Clock.systemUTC(),
    private val random: Random = SecureRandom(byteArrayOf()),
    private val appFn: WiretapContext.() -> HttpHandler,
) : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {
    constructor(app: HttpHandler, renderMode: RenderMode = OnFailure)
        : this(
        renderMode = renderMode,
        random = SecureRandom(byteArrayOf()),
        appFn = { ClientFilters.OpenTelemetryTracing(otel()).then(app) })

    private val state = AtomicReference<TestState>()

    val traceStore get() = state.get().traceStore
    val logStore get() = state.get().logStore
    val transactionStore get() = state.get().transactionStore
    val capturedStdOut get() = state.get().stdOutCapture.toString()
    val capturedStdErr get() = state.get().stdErrCapture.toString()

    private var originalOut: PrintStream? = null
    private var originalErr: PrintStream? = null

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) =
        pc.parameter.parameterizedType.typeName ==
            "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any = state.get().handler

    override fun beforeTestExecution(context: ExtensionContext) {
        val stdOutCapture = ByteArrayOutputStream()
        val stdErrCapture = ByteArrayOutputStream()
        originalOut = System.out
        originalErr = System.err
        System.setOut(PrintStream(TeeOutputStream(originalOut!!, stdOutCapture)))
        System.setErr(PrintStream(TeeOutputStream(originalErr!!, stdErrCapture)))

        val traceStore = TraceStore.InMemory()
        val logStore = LogStore.InMemory()
        val transactionStore = TransactionStore.InMemory()
        val outboundHttp = ResponseFilters.ReportHttpTransaction(clock) { tx ->
            transactionStore.record(tx, Direction.Outbound)
        }.then(httpClient)
        val setup = WiretapContext(outboundHttp, clock, random) { WiretapOpenTelemetry(traceStore, logStore, it) }
        val app = ResponseFilters.ReportHttpTransaction(clock) { tx ->
            transactionStore.record(tx, Direction.Inbound)
        }.then(setup.appFn())

        state.set(TestState(app, traceStore, logStore, transactionStore, stdOutCapture, stdErrCapture))
    }

    override fun afterTestExecution(context: ExtensionContext) {
        originalOut?.let { System.setOut(it) }
        originalErr?.let { System.setErr(it) }
        originalOut = null
        originalErr = null

        val shouldReport = when (renderMode) {
            Always -> true
            OnFailure -> context.executionException.isPresent
            Never -> false
        }

        if (!shouldReport) return

        val testClass = context.requiredTestClass
        val testName = "${testClass.simpleName}.${context.requiredTestMethod.name}"
        val packageDir = testClass.packageName.replace('.', '/')
        val (_, traceStore, logStore, transactionStore, stdOutCapture, stdErrCapture) = state.get()
        val file = renderTestReport(testName, packageDir, traceStore, logStore, transactionStore, stdOutCapture.toString(), stdErrCapture.toString())
        println("Wiretap report: file://${file.absolutePath}")
    }

    private data class TestState(val handler: HttpHandler, val traceStore: TraceStore, val logStore: LogStore, val transactionStore: TransactionStore, val stdOutCapture: ByteArrayOutputStream, val stdErrCapture: ByteArrayOutputStream)
}

private val outputDir by lazy {
    File(System.getProperty("java.io.tmpdir"), "wiretap/${java.time.LocalDateTime.now().toString().replace(":", "-")}")
        .apply { mkdirs() }
}

internal fun renderTestReport(testName: String, packageDir: String, traceStore: TraceStore, logStore: LogStore, transactionStore: TransactionStore, stdOut: String = "", stdErr: String = ""): File {
    val html = Templates()
    val css = Intercept::class.java.classLoader.getResourceAsStream("public/wiretap.css")
        ?.bufferedReader()?.readText() ?: ""

    val traceEntries = traceStore.traces().map { (traceId, spans) ->
        val detail = spans.toTraceDetail(traceId)
        val logsBySpan = logStore.forTrace(traceId).map { it.toSummary(Clock.systemUTC()) }.groupBy { it.spanId }
        val diagram = detail.toSequenceDiagram()
        val ganttHtml = html(TraceDetailView(detail, logsBySpan))
        val diagramHtml = if (diagram.messages.isNotEmpty()) html(TraceDiagramView(diagram.toMermaid())) else ""
        TraceEntry(traceId.value, ganttHtml, diagramHtml)
    }

    val trafficEntries = transactionStore.list().map { wtx ->
        val detail = wtx.toDetail(Clock.systemUTC())
        TrafficEntry(html(TransactionDetailView(detail, showImport = false, showHar = false, showOTelLink = false)))
    }

    val fileName = testName.replace(' ', '-')
    val dir = File(outputDir, packageDir).apply { mkdirs() }

    return File(dir, "${fileName}.html")
        .apply { writeText(html(JUnitTestReport(testName, css, traceEntries, trafficEntries, stdOut, stdErr))) }
}

private class TeeOutputStream(private val primary: OutputStream, private val secondary: OutputStream) : OutputStream() {
    override fun write(b: Int) { primary.write(b); secondary.write(b) }
    override fun write(b: ByteArray, off: Int, len: Int) { primary.write(b, off, len); secondary.write(b, off, len) }
    override fun flush() { primary.flush(); secondary.flush() }
}
