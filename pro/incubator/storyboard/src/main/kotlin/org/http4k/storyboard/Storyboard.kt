/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.core.HttpHandler
import org.http4k.format.Moshi
import org.http4k.storyboard.Story.Outcome
import org.http4k.storyboard.Story.Outcome.Aborted
import org.http4k.storyboard.Story.Outcome.Failed
import org.http4k.storyboard.Story.Outcome.Passed
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.TestAbortedException
import org.openqa.selenium.WebDriver
import java.io.File
import java.time.Clock
import java.time.Instant

class Storyboard(
    private val http: HttpHandler,
    private val outputDir: File = File("build/reports/http4k/storyboard"),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val driverFactory: (HttpHandler, Clock) -> WebDriver = { handler, c -> Http4kWebDriver(handler, c) }
) : BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    override fun beforeTestExecution(context: ExtensionContext) {
        store(context).put(RecordingWebDriver::class.java, RecordingWebDriver(driverFactory(http, clock)))
        store(context).put(StartTimeKey, clock.instant())
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val testClass = context.requiredTestClass
        val start = store(context).get(StartTimeKey, Instant::class.java)
        val story = Story(
            title = context.displayName,
            frames = driver(context).frames(),
            className = testClass.simpleName,
            outcome = context.outcome(),
            durationMs = start?.let { clock.instant().toEpochMilli() - it.toEpochMilli() }
        )
        val dataJson = Moshi.asFormatString(story)

        val classDir = File(outputDir, testClass.name.replace('.', '/'))
        classDir.mkdirs()

        val safeMethod = context.requiredTestMethod.name.sanitiseForFs()
        File(classDir, "$safeMethod.json").writeText(dataJson)
        val html = File(classDir, "$safeMethod.html").apply {
            writeText(renderHtml(story, dataJson))
        }

        ClassIndexWriter.rebuild(classDir, testClass.simpleName)
        context.publishReportEntry("storyboard", html.toURI().toString())
    }

    private fun ExtensionContext.outcome(): Outcome =
        executionException.map { e -> if (e is TestAbortedException) Aborted else Failed }.orElse(Passed)

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext): Boolean =
        parameterContext.parameter.type == RecordingWebDriver::class.java

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): RecordingWebDriver =
        driver(context)

    private fun driver(context: ExtensionContext): RecordingWebDriver =
        store(context).get(RecordingWebDriver::class.java, RecordingWebDriver::class.java)!!

    private fun store(context: ExtensionContext) =
        context.getStore(Namespace.create(context.requiredTestClass, context.requiredTestMethod))
}

private const val StartTimeKey = "storyboard.startTime"

private val unsafeFsChars = Regex("""[/\\:*?"<>|]""")

internal fun String.sanitiseForFs(): String =
    replace(unsafeFsChars, "_").trimEnd('.', ' ').ifEmpty { "_" }
