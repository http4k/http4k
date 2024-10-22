package org.http4k.junit

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import java.util.concurrent.TimeUnit.SECONDS


class OpenTracingTestReporting : TestExecutionListener {

    private val openTelemetry: OpenTelemetrySdk?

    init {
        val honeycombApiKey = System.getenv("HONEYCOMB_API_KEY")

        if (honeycombApiKey == null) {
            openTelemetry = null
        } else {
            val resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "test-main")))

            val dataset = System.getenv("HONEYCOMB_DATASET") ?: "local"

            val spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint("https://api.honeycomb.io/v1/traces")
                .addHeader("x-honeycomb-team", honeycombApiKey)
                .addHeader("x-honeycomb-dataset", dataset)
                .setTimeout(30, SECONDS)
                .build()

            val processor = BatchSpanProcessor.builder(spanExporter).build()
                .also {
                    // required to flush the spans before the JVM exits
                    Runtime.getRuntime().addShutdownHook(Thread { println("shutting down processor"); it.close() })
                }

            openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(
                    SdkTracerProvider.builder()
                        .addSpanProcessor(processor)
                        .setResource(resource)
                        .build()
                )
                .build()
        }
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (openTelemetry == null || !testIdentifier.isTest) return

        val tracer: Tracer = openTelemetry.getTracer("http4")
        val identifier = parseTestIdentifier(testIdentifier.uniqueId)

        val span = tracer.spanBuilder(identifier.spanName()).startSpan()

        try {
            span.makeCurrent().use { _ ->
                span.setAttribute("status", testExecutionResult.status.toString())
                span.setAttribute("junit_id", testIdentifier.uniqueId)

                when (identifier) {
                    is BetterTestIdentifier.TestId -> {
                        span.setAttribute("identified", true)
                        span.setAttribute("package", identifier.packageName)
                        span.setAttribute("class", identifier.className)
                        span.setAttribute("method", identifier.methodName)
                    }

                    is BetterTestIdentifier.Unknown -> {
                        span.setAttribute("identified", false)
                    }
                }
                testExecutionResult.throwable.ifPresent {
                    span.recordException(it)
                }
            }
        } catch (t: Throwable) {
            span.recordException(t)
            throw t
        } finally {
            span.end()
        }
    }
}

fun parseTestIdentifier(identifier: String): BetterTestIdentifier {
    val regex = Regex("""\[class:(.+?)\]/\[method|test-template:(.+?)\]""")
    val matchResult = regex.find(identifier)
    return matchResult?.let {
        val classFullyClassifiedName = it.groupValues[1]
        val methodName = it.groupValues[2].substringBeforeLast('(')
        val packageName = classFullyClassifiedName.substringBeforeLast('.')
        val className = classFullyClassifiedName.substringAfterLast('.')
        BetterTestIdentifier.TestId(packageName, className, methodName)
    } ?: BetterTestIdentifier.Unknown(identifier)
}

sealed class BetterTestIdentifier {
    fun spanName() = when (this) {
        is TestId -> "$packageName.$className.$methodName"
        is Unknown -> identifier
    }

    data class Unknown(val identifier: String) : BetterTestIdentifier()
    data class TestId(val packageName: String, val className: String, val methodName: String) : BetterTestIdentifier()
}
