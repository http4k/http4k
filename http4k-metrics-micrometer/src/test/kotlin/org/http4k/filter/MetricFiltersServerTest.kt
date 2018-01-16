package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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
import org.junit.Test

class MetricFiltersServerTest {
    private val registry = SimpleMeterRegistry()
    private val clock = TickingClock
    private var requestTimer = MetricFilters.Server.RequestTimer(registry, clock = clock)
    private var requestCounter = MetricFilters.Server.RequestCounter(registry)
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
        server(Request(GET, "/timed/one")) shouldMatch hasStatus(OK)
        repeat(2) {
            server(Request(POST, "/timed/two/bob")) shouldMatch (hasStatus(OK) and hasBody("bob"))
        }

        assert(registry,
                hasRequestTimer(GET, "timed_one", OK, 1, 1),
                hasRequestTimer(POST, "timed_two_name", OK, 2, 2)
        )
    }

    @Test
    fun `routes with counter generate request count metrics tagged with path and method and status`() {
        server(Request(GET, "/counted/one")) shouldMatch hasStatus(OK)
        repeat(2) {
            server(Request(POST, "/counted/two/bob")) shouldMatch (hasStatus(OK) and hasBody("bob"))
        }

        assert(registry,
                hasRequestCounter(GET, "counted_one", OK, 1),
                hasRequestCounter(POST, "counted_two_name", OK, 2)
        )
    }

    @Test
    fun `routes without metrics generate nothing`() {
        server(Request(GET, "/unmetered/one")) shouldMatch hasStatus(OK)
        server(Request(DELETE, "/unmetered/two")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR)

        assert(registry,
                hasNoRequestTimer(GET, "unmetered_one", OK),
                hasNoRequestTimer(DELETE, "unmetered_two", INTERNAL_SERVER_ERROR),
                hasNoRequestCounter(GET, "unmetered_one", OK),
                hasNoRequestCounter(DELETE, "unmetered_two", INTERNAL_SERVER_ERROR)
        )
    }

    @Test
    fun `request timer meter names and request id formatter can be configured`() {
        requestTimer = MetricFilters.Server.RequestTimer(registry, "custom.requests", "custom.description",
                "customMethod", "customStatus", "customPath",
                { MetricFilters.Server.defaultRequestIdFormatter(it).plus("-custom") }, clock)

        server(Request(GET, "/timed/one")) shouldMatch hasStatus(OK)

        assert(registry,
                hasRequestTimer(GET, "timed_one-custom", OK, 1, 1,
                        "custom.requests", "custom.description", "customMethod",
                        "customStatus", "customPath")
        )
    }

    @Test
    fun `request counter meter names and request id formatter can be configured`() {
        requestCounter = MetricFilters.Server.RequestCounter(registry, "custom.requests", "custom.description",
                "customMethod", "customStatus", "customPath",
                { MetricFilters.Server.defaultRequestIdFormatter(it).plus("-custom") })

        server(Request(GET, "/counted/one")) shouldMatch hasStatus(OK)

        assert(registry,
                hasRequestCounter(GET, "counted_one-custom", OK, 1,
                        "custom.requests", "custom.description", "customMethod",
                        "customStatus", "customPath")
        )
    }

    @Test
    fun `timed routes without uri template generate request timing metrics tagged with unmapped path value`() {
        server(Request(GET, "/otherTimed/test.json")) shouldMatch hasStatus(OK)
        assert(registry, hasRequestTimer(GET, "UNMAPPED", OK, 1, 1))
    }

    @Test
    fun `counted routes without uri template generate request count metrics tagged with unmapped path value`() {
        server(Request(GET, "/otherCounted/test.json")) shouldMatch hasStatus(OK)
        assert(registry, hasRequestCounter(GET, "UNMAPPED", OK, 1))
    }

    private fun hasRequestCounter(method: Method, path: String, status: Status, count: Long,
                                  name: String = "http.server.request.count",
                                  description: String = "Total number of server requests",
                                  methodName: String = "method",
                                  statusName: String = "status",
                                  requestIdName: String = "path") = hasCounter(name,
            Tags.zip(methodName, method.name, statusName, status.code.toString(), requestIdName, path),
            description(description) and counterCount(count)
    )

    private fun hasRequestTimer(method: Method, path: String, status: Status, count: Long, totalTimeSec: Long,
                                name: String = "http.server.request.latency",
                                description: String = "Timing of server requests",
                                methodName: String = "method",
                                statusName: String = "status",
                                requestIdName: String = "path") = hasTimer(name,
        Tags.zip(methodName, method.name, statusName, status.code.toString(), requestIdName, path),
        description(description) and timerCount(count) and timerTotalTime(totalTimeSec * 1000L)
    )

    private fun hasNoRequestTimer(method: Method, path: String, status: Status) =
            hasTimer("http.server.request.latency",
                    Tags.zip("path", path, "method", method.name, "status", status.code.toString())
            ).not()

    private fun hasNoRequestCounter(method: Method, path: String, status: Status) =
            hasCounter("http.server.request.count",
                    Tags.zip("path", path, "method", method.name, "status", status.code.toString())
            ).not()
}
