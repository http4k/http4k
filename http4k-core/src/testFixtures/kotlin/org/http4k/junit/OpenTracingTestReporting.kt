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
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.SECONDS


class OpenTracingTestReporting : TestExecutionListener {

    private val openTelemetry: OpenTelemetrySdk?
    private val stats = Stats()

    init {
        val honeycombApiKey = System.getenv("HONEYCOMB_API_KEY")

        if (honeycombApiKey == null) {
            openTelemetry = null
        } else {
            val resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "test-main")))

            val dataset = System.getenv("HONEYCOMB_DATASET") ?: "test-main"

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

    override fun executionStarted(testIdentifier: TestIdentifier) = stats.start(testIdentifier)

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (openTelemetry == null || !testIdentifier.isTest) return

        stats.end(testIdentifier)

        val tracer: Tracer = openTelemetry.getTracer("http4")
        val betterIdentifier = parseTestIdentifier(testIdentifier.uniqueId)

        val span = tracer.spanBuilder(betterIdentifier.spanName()).startSpan()

        try {
            span.makeCurrent().use { _ ->
                span.setAttribute("status", testExecutionResult.status.toString())
                span.setAttribute("junit_id", testIdentifier.uniqueId)
                span.setAttribute("duration_ms", stats.duration(testIdentifier) ?: -1)

                when (betterIdentifier) {
                    is BetterTestIdentifier.TestId -> {
                        span.setAttribute("identified", true)
                        span.setAttribute("package", betterIdentifier.packageName)
                        span.setAttribute("class", betterIdentifier.className)
                        span.setAttribute("method", betterIdentifier.methodName)
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
    val regex = Regex("""\[class:(.+?)\]\/\[(method|template):(.+?)\]""")
    val matchResult = regex.find(identifier)
    return matchResult?.let {
        val classFullyClassifiedName = it.groupValues[1]
        val methodName = it.groupValues[3].substringBeforeLast('(')
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

private class Stats {
    private val starts = ConcurrentHashMap<TestIdentifier, Instant>()
    private val ends = ConcurrentHashMap<TestIdentifier, Instant>()

    fun start(testId: TestIdentifier) {
        starts[testId] = Instant.now()
    }

    fun end(testId: TestIdentifier) {
        ends[testId] = Instant.now()
    }

    fun duration(testId: TestIdentifier) =
        starts[testId]?.let { start ->
            ends[testId]?.let { end ->
                end.toEpochMilli() - start.toEpochMilli()
            }
        }
}
