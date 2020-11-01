package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import io.opentelemetry.common.Labels
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Test

class OpenTelemetryMetricsClientTest {

    private val clock = TickingClock
    private var requestTimer = ClientFilters.OpenTelemetryMetrics.RequestTimer(clock = clock)
    private var requestCounter = ClientFilters.OpenTelemetryMetrics.RequestCounter(clock = clock)

    private val remoteServerMock: HttpHandler = {
        when (it.uri.path) {
            "/one" -> Response(OK)
            else -> Response(NOT_FOUND)
        }
    }

    private val timedClient by lazy { requestTimer.then(remoteServerMock) }
    private val countedClient by lazy { requestCounter.then(remoteServerMock) }

    @Test
    fun `timed requests generate timing metrics tagged with method and status and host`() {
        assertThat(timedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))
        repeat(2) {
            assertThat(timedClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        val data = exportMetricsFromOpenTelemetry()
        assertThat(data,
            hasRequestTimer(1, 1000.0, Labels.of("host", "test_server_com", "method", "GET", "status", "200"),
                "http.client.request.latency"))
        assertThat(data,
            hasRequestTimer(1, 1000.0, Labels.of("host", "test_server_com", "method", "GET", "status", "200"),
                "http.client.request.latency"))
        assertThat(data,
            hasRequestTimer(2, 2000.0, Labels.of("host", "another_server_com", "method", "POST", "status", "404"),
                "http.client.request.latency"))
    }

    @Test
    fun `counted requests generate count metrics tagged with method and status and host`() {
        assertThat(countedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))
        repeat(2) {
            assertThat(countedClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestCounter(1, Labels.of("host", "test_server_com", "method", "GET", "status", "200"), "http.client.request.count")
        )
        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestCounter(2, Labels.of("host", "another_server_com", "method", "POST", "status", "404"), "http.client.request.count")
        )
    }

    @Test
    fun `request timer meter names and transaction labelling can be configured`() {
        requestTimer = ClientFilters.OpenTelemetryMetrics.RequestTimer(name = "timer.requests", description = "custom.description", labeler =
        { it.label("foo", "bar") }, clock = clock)

        assertThat(timedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))

        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestTimer(1, 1000.0, Labels.of("foo", "bar"), "timer.requests")
        )
    }

    @Test
    fun `request counter meter names and transaction labelling can be configured`() {
        requestCounter = ClientFilters.OpenTelemetryMetrics.RequestCounter(name = "counter.requests", description = "custom.description", labeler =
        { it.label("foo", "bar") })

        assertThat(countedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))

        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestCounter(1, Labels.of("foo", "bar"), "counter.requests")
        )
    }
}
