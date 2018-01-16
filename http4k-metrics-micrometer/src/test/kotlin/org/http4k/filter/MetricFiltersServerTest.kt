package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
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
                        "/one" bind Method.GET to { Response(Status.OK) },
                        "/two/{name:.*}" bind Method.POST to { Response(Status.OK).body(Path.of("name")(it)) }
                ).withFilter(requestTimer),
                "/counted" bind routes(
                        "/one" bind Method.GET to { Response(Status.OK) },
                        "/two/{name:.*}" bind Method.POST to { Response(Status.OK).body(Path.of("name")(it)) }
                ).withFilter(requestCounter),
                "/unmetered" bind routes(
                        "one" bind Method.GET to { Response(Status.OK) },
                        "two" bind Method.DELETE to { Response(Status.INTERNAL_SERVER_ERROR) }
                ),
                "/otherTimed" bind static().withFilter(requestTimer),
                "/otherCounted" bind static().withFilter(requestCounter)
        )
    }

    @Test
    fun `routes with timer generate request timing metrics tagged with path and method and status`() {
        server(Request(Method.GET, "/timed/one")) shouldMatch hasStatus(Status.OK)
        repeat(2) {
            server(Request(Method.POST, "/timed/two/bob")) shouldMatch (hasStatus(Status.OK) and hasBody("bob"))
        }

        assert(registry,
                hasRequestTimer(Method.GET, "timed_one", Status.OK, 1, 1),
                hasRequestTimer(Method.POST, "timed_two_name", Status.OK, 2, 2)
        )
    }

    @Test
    fun `routes with counter generate request count metrics tagged with path and method and status`() {
        server(Request(Method.GET, "/counted/one")) shouldMatch hasStatus(Status.OK)
        repeat(2) {
            server(Request(Method.POST, "/counted/two/bob")) shouldMatch (hasStatus(Status.OK) and hasBody("bob"))
        }

        assert(registry,
                hasRequestCounter(Method.GET, "counted_one", Status.OK, 1),
                hasRequestCounter(Method.POST, "counted_two_name", Status.OK, 2)
        )
    }

    @Test
    fun `routes without metrics generate nothing`() {
        server(Request(Method.GET, "/unmetered/one")) shouldMatch hasStatus(Status.OK)
        server(Request(Method.DELETE, "/unmetered/two")) shouldMatch hasStatus(Status.INTERNAL_SERVER_ERROR)

        assert(registry,
                hasNoRequestTimer(Method.GET, "unmetered_one", Status.OK),
                hasNoRequestTimer(Method.DELETE, "unmetered_two", Status.INTERNAL_SERVER_ERROR),
                hasNoRequestCounter(Method.GET, "unmetered_one", Status.OK),
                hasNoRequestCounter(Method.DELETE, "unmetered_two", Status.INTERNAL_SERVER_ERROR)
        )
    }

    @Test
    fun `request timer meter names and request id formatter can be configured`() {
        requestTimer = MetricFilters.Server.RequestTimer(registry, "custom.requests", "custom.description",
                "customMethod", "customStatus", "customPath",
                { MetricFilters.Server.defaultRequestIdFormatter(it).plus("-custom") }, clock)

        server(Request(Method.GET, "/timed/one")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                hasRequestTimer(Method.GET, "timed_one-custom", Status.OK, 1, 1,
                        "custom.requests", "custom.description", "customMethod",
                        "customStatus", "customPath")
        )
    }

    @Test
    fun `request counter meter names and request id formatter can be configured`() {
        requestCounter = MetricFilters.Server.RequestCounter(registry, "custom.requests", "custom.description",
                "customMethod", "customStatus", "customPath",
                { MetricFilters.Server.defaultRequestIdFormatter(it).plus("-custom") })

        server(Request(Method.GET, "/counted/one")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                hasRequestCounter(Method.GET, "counted_one-custom", Status.OK, 1,
                        "custom.requests", "custom.description", "customMethod",
                        "customStatus", "customPath")
        )
    }

    @Test
    fun `timed routes without uri template generate request timing metrics tagged with unmapped path value`() {
        server(Request(Method.GET, "/otherTimed/test.json")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                // The count and time seems wrong - is static handler invoked twice per request ???
                hasRequestTimer(Method.GET, "UNMAPPED", Status.OK, 2, 2)
        )
    }

    @Test
    fun `counted routes without uri template generate request count metrics tagged with unmapped path value`() {
        server(Request(Method.GET, "/otherCounted/test.json")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                // The count and time seems wrong - is static handler invoked twice per request ???
                hasRequestCounter(Method.GET, "UNMAPPED", Status.OK, 2)
        )
    }

    private fun hasRequestTimer(method: Method, path: String, status: Status, count: Long, totalTimeSec: Long,
                                name: String = "http.server.requests",
                                description: String = "Timings of server requests",
                                methodName: String = "method",
                                statusName: String = "status",
                                requestIdName: String = "path") = hasTimer(name,
            Tags.zip(methodName, method.name, statusName, status.code.toString(), requestIdName, path),
            description(description) and timerCount(count) and timerTotalTime(totalTimeSec * 1000L)
    )

    private fun hasRequestCounter(method: Method, path: String, status: Status, count: Long,
                                  name: String = "http.server.requests",
                                  description: String = "Total number of server requests",
                                  methodName: String = "method",
                                  statusName: String = "status",
                                  requestIdName: String = "path") = hasCounter(name,
            Tags.zip(methodName, method.name, statusName, status.code.toString(), requestIdName, path),
            description(description) and counterCount(count)
    )

    private fun hasNoRequestTimer(method: Method, path: String, status: Status) =
            hasTimer("http.server.requests",
                    Tags.zip("path", path, "method", method.name, "status", status.code.toString())
            ).not()

    private fun hasNoRequestCounter(method: Method, path: String, status: Status) =
            hasCounter("http.server.requests",
                    Tags.zip("path", path, "method", method.name, "status", status.code.toString())
            ).not()
}
