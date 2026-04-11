/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import io.opentelemetry.api.GlobalOpenTelemetry
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ResponseFilters
import org.http4k.wiretap.Context
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.Ordering
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

/**
 * Wiretap Intercept is a JUnit Extension that records all traffic and telemetry inside an application
 * in order that it can be visualised post-test.
 */
class Intercept @JvmOverloads constructor(
    private val renderMode: RenderMode = OnFailure,
    private val redirectFilter: Filter = Filter.NoOp,
    private val clock: Clock = Clock.systemUTC(),
    private val random: Random = SecureRandom(byteArrayOf()),
    private val serverName: String = "http4k-server",
    private val baseUrl: Uri = Uri.of(""),
    private val appFn: Context.() -> HttpHandler
) : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    companion object {
        fun http(
            renderMode: RenderMode = OnFailure,
            redirectFilter: Filter = Filter.NoOp,
            clock: Clock = Clock.systemUTC(),
            random: Random = SecureRandom(byteArrayOf()),
            serverName: String = "http4k-server",
            baseUrl: Uri = Uri.of(""),
            appFn: Context.() -> HttpHandler
        ) = Intercept(renderMode, redirectFilter, clock, random, serverName, baseUrl, appFn)

        fun poly(
            renderMode: RenderMode = OnFailure,
            redirectFilter: Filter = Filter.NoOp,
            clock: Clock = Clock.systemUTC(),
            random: Random = SecureRandom(byteArrayOf()),
            serverName: String = "http4k-server",
            baseUrl: Uri = Uri.of(""),
            appFn: Context.() -> PolyHandler
        ) = Intercept(renderMode, redirectFilter, clock, random, serverName, baseUrl, { appFn().http!! })
    }
    @JvmOverloads constructor(renderMode: RenderMode = OnFailure) : this(renderMode = renderMode, appFn = { http() })

    constructor(app: HttpHandler, renderMode: RenderMode = OnFailure, serverName: String = "http4k-server",)
        : this(
        renderMode = renderMode,
        random = SecureRandom(byteArrayOf()),
        appFn = { ClientFilters.OpenTelemetryTracing(otel(serverName)).then(app) })

    private val state = AtomicReference<TestState>()

    internal val traceStore get() = state.get().traceStore
    internal val logStore get() = state.get().logStore
    internal val transactionStore get() = state.get().transactionStore
    internal val capturedStdOut get() = state.get().stdOutCapture.toString()
    internal val capturedStdErr get() = state.get().stdErrCapture.toString()

    private var originalOut: PrintStream? = null
    private var originalErr: PrintStream? = null

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) =
        pc.parameter.type == ChaosEngine::class.java ||
            pc.parameter.type == McpClient::class.java ||
        pc.parameter.parameterizedType.typeName ==
            "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any =
        when (pc.parameter.type) {
            ChaosEngine::class.java -> state.get().outboundChaos
            McpClient::class.java -> HttpNonStreamingMcpClient(baseUrl.extend(Uri.of("/mcp")), http = state.get().http)
            else -> state.get().http
        }

    override fun beforeTestExecution(context: ExtensionContext) {
        val stdOutCapture = ByteArrayOutputStream()
        val stdErrCapture = ByteArrayOutputStream()
        originalOut = System.out
        originalErr = System.err
        System.setOut(PrintStream(TeeOutputStream(originalOut!!, stdOutCapture)))
        System.setErr(PrintStream(TeeOutputStream(originalErr!!, stdErrCapture)))

        val traceStore = TraceStore.InMemory()
        val logStore = LogStore.InMemory()
        val txStore = TransactionStore.InMemory()

        GlobalOpenTelemetry.resetForTest()
        GlobalOpenTelemetry.set(WiretapOpenTelemetry(traceStore, logStore, clock, serverName))

        val outboundChaos = ChaosEngine()
        val clientFilter = ResponseFilters.ReportHttpTransaction(clock) { tx -> txStore.record(tx, Outbound) }
            .then(outboundChaos)
        val setup = Context(clientFilter, clock, random) { WiretapOpenTelemetry(traceStore, logStore, clock, it) }

        val app = redirectFilter
            .then(ResponseFilters.ReportHttpTransaction(clock) { tx -> txStore.record(tx, Inbound) })
            .then(setup.appFn())

        state.set(TestState(app, outboundChaos, traceStore, logStore, txStore, stdOutCapture, stdErrCapture))
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
        val (_, _, traceStore, logStore, transactionStore, stdOutCapture, stdErrCapture) = state.get()
        val file = renderTestReport(testName, packageDir, traceStore, logStore, transactionStore, stdOutCapture.toString(), stdErrCapture.toString())

        println("Wiretap report: file://${file.absolutePath}")
        context.publishReportEntry("wiretap", "file://${file.absolutePath}")
    }

    private data class TestState(
        val http: HttpHandler,
        val outboundChaos: ChaosEngine,
        val traceStore: TraceStore,
        val logStore: LogStore,
        val transactionStore: TransactionStore,
        val stdOutCapture: ByteArrayOutputStream,
        val stdErrCapture: ByteArrayOutputStream
    )
}

private val outputDir by lazy {
    File("build/reports/wiretap").apply { mkdirs() }
}

internal fun renderTestReport(testName: String, packageDir: String, traceStore: TraceStore, logStore: LogStore, transactionStore: TransactionStore, stdOut: String = "", stdErr: String = ""): File {
    val html = Templates()
    val css = Intercept::class.java.classLoader.getResourceAsStream("public/wiretap.css")
        ?.bufferedReader()?.readText() ?: ""

    val traceEntries = traceStore.traces(Ordering.Ascending).map { (traceId, spans) ->
        val detail = spans.toTraceDetail(traceId)
        val logsBySpan = logStore.forTrace(traceId).map { it.toSummary(Clock.systemUTC()) }.groupBy { it.spanId }
        val diagram = detail.toSequenceDiagram()
        val ganttHtml = html(TraceDetailView(detail, logsBySpan))
        val diagramHtml = if (diagram.messages.isNotEmpty()) html(TraceDiagramView(diagram.toMermaid())) else ""
        TraceEntry(traceId.value, ganttHtml, diagramHtml)
    }

    val trafficEntries = transactionStore.list(ordering = Ordering.Ascending, limit = Int.MAX_VALUE).map { wtx ->
        val detail = wtx.toDetail(Clock.systemUTC())
        TrafficEntry(html(TransactionDetailView(detail)))
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
