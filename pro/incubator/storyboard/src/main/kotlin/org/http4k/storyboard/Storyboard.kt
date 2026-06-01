package org.http4k.storyboard

import org.http4k.core.HttpHandler
import org.http4k.format.Moshi
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.time.Clock

class Storyboard(
    private val http: HttpHandler,
    private val outputDir: File = File("build/reports/http4k/storyboard"),
    private val clock: Clock = Clock.systemDefaultZone()
) : BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    override fun beforeTestExecution(context: ExtensionContext) {
        store(context).put(RecordingWebDriver::class.java, RecordingWebDriver(Http4kWebDriver(http, clock)))
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val story = Story(context.displayName, driver(context).frames())
        val dataJson = Moshi.asFormatString(story)
        outputDir.mkdirs()
        val base = "${context.requiredTestClass.name}.${context.requiredTestMethod.name}"

        File(outputDir, "$base.json").writeText(dataJson)
        val html = File(outputDir, "$base.html").apply {
            writeText(renderHtml(story, dataJson))
        }

        context.publishReportEntry("storyboard", html.toURI().toString())
    }

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext): Boolean =
        parameterContext.parameter.type == RecordingWebDriver::class.java

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): RecordingWebDriver =
        driver(context)

    private fun driver(context: ExtensionContext): RecordingWebDriver =
        store(context).get(RecordingWebDriver::class.java, RecordingWebDriver::class.java)!!

    private fun store(context: ExtensionContext) =
        context.getStore(Namespace.create(context.requiredTestClass, context.requiredTestMethod))
}
