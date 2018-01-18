package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.util.TickingClock
import org.junit.Test

class MetricFiltersClientTest {

    private val registry = SimpleMeterRegistry()
    private val clock = TickingClock
    private var requestTimer = MetricFilters.Client.RequestTimer(registry, clock = clock)
    private var requestCounter = MetricFilters.Client.RequestCounter(registry)
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
        timedClient(Request(GET, "http://test.server.com:9999/one")) shouldMatch hasStatus(OK)
        repeat(2) {
            timedClient(Request(POST, "http://another.server.com:8888/missing")) shouldMatch hasStatus(NOT_FOUND)
        }

        assert(registry,
            hasRequestTimer(GET, OK, "test_server_com", 1, 1),
            hasRequestTimer(POST, NOT_FOUND, "another_server_com", 2, 2)
        )
    }

    @Test
    fun `counted requests generate count metrics tagged with method and status and host`() {
        countedClient(Request(GET, "http://test.server.com:9999/one")) shouldMatch hasStatus(OK)
        repeat(2) {
            countedClient(Request(POST, "http://another.server.com:8888/missing")) shouldMatch hasStatus(NOT_FOUND)
        }

        assert(registry,
            hasRequestCounter(GET, OK, "test_server_com", 1),
            hasRequestCounter(POST, NOT_FOUND, "another_server_com", 2)
        )
    }

    @Test
    fun `request timer meter names and request id formatter can be configured`() {
        requestTimer = MetricFilters.Client.RequestTimer(registry, "custom.requests", "custom.description",
            "customMethod", "customStatus", "customHost",
            { MetricFilters.Client.defaultRequestIdFormatter(it).plus("-custom") }, clock)

        timedClient(Request(GET, "http://test.server.com:9999/one")) shouldMatch hasStatus(OK)

        assert(registry,
            hasRequestTimer(GET, OK, "test_server_com-custom", 1, 1,
                "custom.requests", "custom.description", "customMethod",
                "customStatus", "customHost")
        )
    }

    @Test
    fun `request counter meter names and request id formatter can be configured`() {
        requestCounter = MetricFilters.Client.RequestCounter(registry, "custom.requests", "custom.description",
            "customMethod", "customStatus", "customHost",
            { MetricFilters.Client.defaultRequestIdFormatter(it).plus("-custom") })

        countedClient(Request(GET, "http://test.server.com:9999/one")) shouldMatch hasStatus(OK)

        assert(registry,
            hasRequestCounter(GET, OK, "test_server_com-custom", 1,
                "custom.requests", "custom.description", "customMethod",
                "customStatus", "customHost")
        )
    }

    private fun hasRequestCounter(method: Method, status: Status, host: String, count: Long,
                                  name: String = "http.client.request.count",
                                  description: String = "Total number of client requests",
                                  methodName: String = "method",
                                  statusName: String = "status",
                                  requestIdName: String = "host") = hasCounter(name,
        Tags.zip(methodName, method.name, statusName, status.code.toString(), requestIdName, host),
        description(description) and counterCount(count)
    )

    private fun hasRequestTimer(method: Method, status: Status, host: String, count: Long, totalTimeSec: Long,
                                name: String = "http.client.request.latency",
                                description: String = "Timing of client requests",
                                methodName: String = "method",
                                statusName: String = "status",
                                requestIdName: String = "host") = hasTimer(name,
        Tags.zip(methodName, method.name, statusName, status.code.toString(), requestIdName, host),
        description(description) and timerCount(count) and timerTotalTime(totalTimeSec * 1000)
    )
}