package org.http4k.filter

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import io.opentelemetry.api.common.Labels
import io.opentelemetry.sdk.metrics.data.MetricData
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Test

class OpenTelemetryMetricsServerTest {
    private val clock = TickingClock
    private var requestTimer = ServerFilters.OpenTelemetryMetrics.RequestTimer(clock = clock)
    private var requestCounter = ServerFilters.OpenTelemetryMetrics.RequestCounter(clock = clock)
    private val server by lazy {
        routes(
            "/timed" bind routes(
                "/one" bind GET to { Response(OK) },
                "/two/{name:.*}" bind POST to { Response(OK).body(Path.of("name")(it)) }
            ).withFilter(requestTimer),
            "/counted" bind routes(
                "/one" bind GET to { Response(OK) },
                "/two/{name:.*}" bind POST to { Response(OK).body(Path.of("name")(it)) }
            ).withFilter(requestCounter),
            "/unmetered" bind routes(
                "one" bind GET to { Response(OK) },
                "two" bind DELETE to { Response(INTERNAL_SERVER_ERROR) }
            ),
            "/otherTimed" bind static().withFilter(requestTimer),
            "/otherCounted" bind static().withFilter(requestCounter)
        )
    }

    @Test
    fun `routes with timer generate request timing metrics tagged with path and method and status`() {
        assertThat(server(Request(GET, "/timed/one")), hasStatus(OK))
        repeat(2) {
            assertThat(server(Request(POST, "/timed/two/bob")), (hasStatus(OK) and hasBody("bob")))
        }

        val data = exportMetricsFromOpenTelemetry()
        assertThat(data, hasRequestTimer(1, 1000.0, Labels.of("path", "timed_one", "method", "GET", "status", "200")))
        assertThat(data, hasRequestTimer(2, 2000.0, Labels.of("path", "timed_two_name", "method", "POST", "status", "200")))
    }

    @Test
    fun `routes with counter generate request count metrics tagged with path and method and status`() {
        assertThat(server(Request(GET, "/counted/one")), hasStatus(OK))
        repeat(2) {
            assertThat(server(Request(POST, "/counted/two/bob")), (hasStatus(OK) and hasBody("bob")))
        }

        val data = exportMetricsFromOpenTelemetry()
        assertThat(data, hasRequestCounter(1, Labels.of("path", "counted_one", "method", "GET", "status", "200")))
        assertThat(data, hasRequestCounter(2, Labels.of("path", "counted_two_name", "method", "POST", "status", "200")))
    }

    @Test
    fun `routes without metrics generate nothing`() {
        assertThat(server(Request(GET, "/unmetered/one")), hasStatus(OK))
        assertThat(server(Request(DELETE, "/unmetered/two")), hasStatus(INTERNAL_SERVER_ERROR))

        val data = exportMetricsFromOpenTelemetry()

        assertThat(data, hasNoRequestTimer(GET, "unmetered_one", OK))
        assertThat(data, hasNoRequestTimer(DELETE, "unmetered_two", INTERNAL_SERVER_ERROR))
        assertThat(data, hasNoRequestCounter(GET, "unmetered_one", OK))
        assertThat(data, hasNoRequestCounter(DELETE, "unmetered_two", INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `request timer meter names and request id formatter can be configured`() {
        requestTimer = ServerFilters.OpenTelemetryMetrics.RequestTimer(name = "custom.requests", description = "custom.description", labeler = { it.label("foo", "bar") })

        assertThat(server(Request(GET, "/timed/one")), hasStatus(OK))

        val data = exportMetricsFromOpenTelemetry()
        assertThat(data,
            hasRequestTimer(1, 1000.0, Labels.of("foo", "bar", "routingGroup", "timed/one"), "custom.requests")
        )
    }

    @Test
    fun `request counter meter names and request id formatter can be configured`() {
        requestCounter = ServerFilters.OpenTelemetryMetrics.RequestCounter(name = "custom.requests2", description = "custom.description", labeler = { it.label("foo", "bar") })

        assertThat(server(Request(GET, "/counted/one")), hasStatus(OK))

        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestCounter(1, Labels.of("foo", "bar", "routingGroup", "counted/one"), "custom.requests2")
        )
    }

    @Test
    fun `timed routes without uri template generate request timing metrics tagged with unmapped path value`() {
        assertThat(server(Request(GET, "/otherTimed/test.json")), hasStatus(OK))

        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestTimer(1, 1000.0, Labels.of("path", "UNMAPPED", "method", "GET", "status", "200"))
        )
    }

    @Test
    fun `counted routes without uri template generate request count metrics tagged with unmapped path value`() {
        assertThat(server(Request(GET, "/otherCounted/test.json")), hasStatus(OK))
        assertThat(exportMetricsFromOpenTelemetry(),
            hasRequestCounter(1, Labels.of("path", "UNMAPPED", "method", "GET", "status", "200")))
    }

    private fun hasNoRequestTimer(method: Method, path: String, status: Status) =
        object : Matcher<List<MetricData>> {
            override val description = "http.server.request.latency"

            override fun invoke(actual: List<MetricData>): MatchResult =
                if (actual.firstOrNull { it.name == description }
                        ?.points
                        ?.any { it.labels == Labels.of("path", path, "method", method.name, "status", status.code.toString()) } != true) Match else Mismatch(actual.toString())
        }
}
