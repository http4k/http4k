package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.ErrorAttributes
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.NetworkAttributes
import io.opentelemetry.semconv.ServerAttributes
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

class OpenTelemetry2xMetricsClientTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            GlobalOpenTelemetry.resetForTest()
            setupOpenTelemetryMeterProvider()
        }
    }

    private val clock = TickingClock()
    private val requestDuration = ClientFilters.OpenTelemetry2xMetrics.RequestDuration(clock = clock)

    private val remoteServerMock: HttpHandler = routes("/one.json" bind GET to { Response(OK) })

    private val durationClient by lazy { requestDuration.then(remoteServerMock) }

    @Test
    fun `requests generate duration metrics tagged with method and status and host`() {
        assertThat(durationClient(Request(GET, "http://test.server.com:9999/one.json")), hasStatus(OK))
        repeat(2) {
            assertThat(durationClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasClientRequestDuration(
                1,
                1000.0,
                Attributes.of(
                    ServerAttributes.SERVER_ADDRESS, "test.server.com",
                    stringKey("http.request.method"), "GET",
                    longKey("http.response.status_code"), 200,
                    ServerAttributes.SERVER_PORT, 9999,
                    NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"
                )
            )
        )
        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasClientRequestDuration(
                1,
                1000.0,
                Attributes.of(
                    ServerAttributes.SERVER_ADDRESS, "test.server.com",
                    stringKey("http.request.method"), "GET",
                    HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200,
                    ServerAttributes.SERVER_PORT, 9999,
                    NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"
                )
            )
        )
        assertThat(
            exportMetricsFromOpenTelemetry(),
            hasClientRequestDuration(
                2,
                2000.0,
                Attributes.of(
                    ServerAttributes.SERVER_ADDRESS, "another.server.com",
                    stringKey("http.request.method"), "POST",
                    longKey("http.response.status_code"), 404,
                    ServerAttributes.SERVER_PORT, 8888,
                    NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1",
                    ErrorAttributes.ERROR_TYPE, "Not Found"
                )
            )
        )
    }
}
