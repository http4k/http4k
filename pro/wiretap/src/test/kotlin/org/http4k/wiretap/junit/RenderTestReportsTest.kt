package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.otel.WiretapOpenTelemetry
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class RenderTestReportsTest {

    private val app = routes("/" bind Method.GET to { Response(OK).body("hello") })

    @Test
    fun `renders standalone HTML report containing all traces`() {
        val traceStore = TraceStore.InMemory()
        val logStore = LogStore.InMemory()
        val transactionStore = TransactionStore.InMemory()
        val handler = ServerFilters.OpenTelemetryTracing(WiretapOpenTelemetry(traceStore, logStore)).then(app)

        handler(Request(Method.GET, "/"))

        val file = renderTestReport("TestClass.test method", "com/example", traceStore, logStore, transactionStore)

        assertThat(file.name, equalTo("TestClass.test-method.html"))
        assertThat(file.parentFile.path.endsWith("com/example"), equalTo(true))

        val content = file.readText()
        assertThat("should be standalone HTML", content.contains("<!DOCTYPE html>"), equalTo(true))
        assertThat("should contain gantt chart", content.contains("gantt-chart"), equalTo(true))
        assertThat("should contain mermaid", content.contains("mermaid"), equalTo(true))
        assertThat("should contain bootstrap css", content.contains("bootstrap"), equalTo(true))

        file.delete()
    }

    @Test
    fun `renders empty report when no traces recorded`() {
        val file = renderTestReport("Empty.test", "com/example", TraceStore.InMemory(), LogStore.InMemory(), TransactionStore.InMemory())

        val content = file.readText()
        assertThat("should be standalone HTML", content.contains("<!DOCTYPE html>"), equalTo(true))
        assertThat("should contain test name", content.contains("Empty.test"), equalTo(true))

        file.delete()
    }

    @Test
    fun `renders stdout and stderr tabs with captured output`() {
        val file = renderTestReport("Output.test", "com/example", TraceStore.InMemory(), LogStore.InMemory(), TransactionStore.InMemory(), stdOut = "stdout line", stdErr = "stderr line")
        val content = file.readText()

        assertThat("should contain stdout tab", content.contains("stdout-tab"), equalTo(true))
        assertThat("should contain stderr tab", content.contains("stderr-tab"), equalTo(true))
        assertThat("should contain stdout text", content.contains("stdout line"), equalTo(true))
        assertThat("should contain stderr text", content.contains("stderr line"), equalTo(true))

        file.delete()
    }

    @Test
    fun `renders traffic tab with recorded transactions`() {
        val traceStore = TraceStore.InMemory()
        val logStore = LogStore.InMemory()
        val transactionStore = TransactionStore.InMemory()

        transactionStore.record(
            HttpTransaction(
                request = Request(Method.GET, "/api/test"),
                response = Response(OK).body("test response"),
                start = Instant.now(),
                duration = Duration.ofMillis(42)
            ),
            Direction.Inbound
        )

        val file = renderTestReport("Traffic.test", "com/example", traceStore, logStore, transactionStore)
        val content = file.readText()

        assertThat("should contain traffic tab", content.contains("traffic-tab"), equalTo(true))
        assertThat("should not contain traces tab when no traces", content.contains("traces-tab"), equalTo(false))
        assertThat("should contain request path", content.contains("/api/test"), equalTo(true))
        assertThat("should contain response body", content.contains("test response"), equalTo(true))

        file.delete()
    }
}
