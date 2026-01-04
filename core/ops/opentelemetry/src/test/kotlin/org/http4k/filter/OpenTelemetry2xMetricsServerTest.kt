package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.NetworkAttributes
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.util.TickingClock
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OpenTelemetry2xMetricsServerTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            GlobalOpenTelemetry.resetForTest()
            setupOpenTelemetryMeterProvider()
        }
    }

    private val clock = TickingClock()
    private var requestDuration = ServerFilters.OpenTelemetry2xMetrics.RequestDuration(clock = clock)
    private val server by lazy {
        routes(
            "/duration" bind routes(
                "/one" bind GET to { Response(OK) },
                "/two/{name:.*}" bind POST to { Response(OK).body(Path.of("name")(it)) },
            ).withFilter(requestDuration),
            "/unmetered" bind routes(
                "one" bind GET to { Response(OK) },
                "two" bind DELETE to { Response(INTERNAL_SERVER_ERROR) }
            ),
        )
    }

    @Test
    fun `routes with duration generate request duration metrics tagged with path and method and status`() {
        assertThat(server(Request(GET, "/duration/one")), hasStatus(OK))
        repeat(2) {
            assertThat(server(Request(POST, "/duration/two/bob")), (hasStatus(OK) and hasBody("bob")))
        }

        val data = exportMetricsFromOpenTelemetry()

        assertThat(data, hasRequestDurationUnitOf("s"))
        assertThat(
            data,
            hasRequestDuration(
                1,
                1000.0,
                Attributes.of(
                    stringKey("http.route"), "/duration/one",
                    stringKey("http.request.method"), "GET",
                    HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200,
                    NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1",
                )
            )
        )
        assertThat(
            data,
            hasRequestDuration(
                2,
                2000.0,
                Attributes.of(
                    stringKey("http.route"), "/duration/two/:name",
                    stringKey("http.request.method"), "POST",
                    HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200,
                    NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1",
                )
            )
        )
    }

    @Test
    fun `routes without metrics generate nothing`() {
        assertThat(server(Request(GET, "/unmetered/one")), hasStatus(OK))
        assertThat(server(Request(DELETE, "/unmetered/two")), hasStatus(INTERNAL_SERVER_ERROR))

        val data = exportMetricsFromOpenTelemetry()

        assertThat(data, hasNoRequestDurationWithStatus(OK))
        assertThat(data, hasNoRequestDurationWithStatus(INTERNAL_SERVER_ERROR))
    }
}
