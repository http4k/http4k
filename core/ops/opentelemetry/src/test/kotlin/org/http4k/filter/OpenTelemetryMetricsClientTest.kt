package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.util.TickingClock
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OpenTelemetryMetricsClientTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            GlobalOpenTelemetry.resetForTest()
            setupOpenTelemetryMeterProvider()
        }
    }

    private val clock = TickingClock()
    private var requestTimer = ClientFilters.OpenTelemetryMetrics.RequestTimer(clock = clock)
    private var requestCounter = ClientFilters.OpenTelemetryMetrics.RequestCounter(clock = clock)

    private val remoteServerMock: HttpHandler = routes("/one.json" bind GET to { Response(OK) })

    private val timedClient by lazy { requestTimer.then(remoteServerMock) }
    private val countedClient by lazy { requestCounter.then(remoteServerMock) }

    @Test
    fun `timed requests generate timing metrics tagged with method and status and host`() {
        assertThat(timedClient(Request(GET, "http://test.server.com:9999/one.json")), hasStatus(OK))
        repeat(2) {
            assertThat(timedClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestTimer(
                1,
                1000.0,
                Attributes.of(
                    stringKey("host"),
                    "test_server_com",
                    stringKey("method"),
                    "GET",
                    stringKey("status"),
                    "200",
                    stringKey("path"),
                    "one_json"
                ),
                "http.client.request.latency"
            )
        )
        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestTimer(
                1,
                1000.0,
                Attributes.of(
                    stringKey("host"),
                    "test_server_com",
                    stringKey("method"),
                    "GET",
                    stringKey("status"),
                    "200",
                    stringKey("path"),
                    "one_json"
                ),
                "http.client.request.latency"
            )
        )
        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestTimer(
                2,
                2000.0,
                Attributes.of(
                    stringKey("host"),
                    "another_server_com",
                    stringKey("method"),
                    "POST",
                    stringKey("status"),
                    "404",
                    stringKey("path"),
                    "UNMAPPED"
                ),
                "http.client.request.latency"
            )
        )
    }

    @Test
    fun `counted requests generate count metrics tagged with method and status and host`() {
        assertThat(countedClient(Request(GET, "http://test.server.com:9999/one.json")), hasStatus(OK))
        repeat(2) {
            assertThat(countedClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestCounter(
                1,
                Attributes.of(
                    stringKey("host"),
                    "test_server_com",
                    stringKey("method"),
                    "GET",
                    stringKey("status"),
                    "200",
                    stringKey("path"),
                    "one_json"

                ),
                "http.client.request.count"
            )
        )
        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestCounter(
                2,
                Attributes.of(
                    stringKey("host"),
                    "another_server_com",
                    stringKey("method"),
                    "POST",
                    stringKey("status"),
                    "404",
                    stringKey("path"),
                    "UNMAPPED"
                ),
                "http.client.request.count"
            )
        )
    }

    @Test
    fun `request timer meter names and transaction labelling can be configured`() {
        requestTimer = ClientFilters.OpenTelemetryMetrics.RequestTimer(
            name = "timer.requests",
            description = "custom.description",
            labeler =
            { it.copy(labels = mapOf("foo" to "bar")) },
            clock = clock
        )

        assertThat(timedClient(Request(GET, "http://test.server.com:9999/one.json")), hasStatus(OK))

        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestTimer(1, 1000.0, Attributes.of(stringKey("foo"), "bar"), "timer.requests")
        )
    }

    @Test
    fun `request counter meter names and transaction labelling can be configured`() {
        requestCounter = ClientFilters.OpenTelemetryMetrics.RequestCounter(name = "counter.requests",
            description = "custom.description",
            labeler =
            { it.copy(labels = mapOf("foo" to "bar")) }
        )

        assertThat(countedClient(Request(GET, "http://test.server.com:9999/one.json")), hasStatus(OK))

        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasRequestCounter(1, Attributes.of(stringKey("foo"), "bar"), "counter.requests")
        )
    }
}
