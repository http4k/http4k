package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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
        assertThat(timedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))
        repeat(2) {
            assertThat(timedClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        assert(registry,
            hasRequestTimer(1, 1, tags = arrayOf("host" to "test_server_com", "method" to "GET", "status" to "200")),
            hasRequestTimer(2, 2, tags = arrayOf("host" to "another_server_com", "method" to "POST", "status" to "404"))
        )
    }

    @Test
    fun `counted requests generate count metrics tagged with method and status and host`() {
        assertThat(countedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))
        repeat(2) {
            assertThat(countedClient(Request(POST, "http://another.server.com:8888/missing")), hasStatus(NOT_FOUND))
        }

        assert(registry,
            hasRequestCounter(1, tags = arrayOf("host" to "test_server_com", "method" to "GET", "status" to "200")),
            hasRequestCounter(2, tags = arrayOf("host" to "another_server_com", "method" to "POST", "status" to "404"))
        )
    }

    @Test
    fun `request timer meter names and transaction labelling can be configured`() {
        requestTimer = MetricFilters.Client.RequestTimer(registry, "custom.requests", "custom.description",
            { it.label("foo", "bar") }, clock)

        assertThat(timedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))

        assert(registry,
            hasRequestTimer(1, 1, "custom.requests", "custom.description", tags = arrayOf("foo" to "bar"))
        )
    }

    @Test
    fun `request counter meter names and transaction labelling can be configured`() {
        requestCounter = MetricFilters.Client.RequestCounter(registry, "custom.requests", "custom.description",
            { it.label("foo", "bar") })

        assertThat(countedClient(Request(GET, "http://test.server.com:9999/one")), hasStatus(OK))

        assert(registry,
            hasRequestCounter(1, "custom.requests", "custom.description",
                "foo" to "bar"
            )
        )
    }

    private fun hasRequestCounter(
        count: Long,
        name: String = "http.client.request.count",
        description: String = "Total number of client requests",
        vararg tags: Pair<String, String>
    ) = hasCounter(name,
        tags.asList()
            .map { Tag.of(it.first, it.second) },
        description(description) and counterCount(count)
    )

    private fun hasRequestTimer(
        count: Long,
        totalTimeSec: Long,
        name: String = "http.client.request.latency",
        description: String = "Timing of client requests",
        vararg tags: Pair<String, String>
    ) = hasTimer(name,
        tags.asList()
            .map { Tag.of(it.first, it.second) },
        description(description) and timerCount(count) and timerTotalTime(totalTimeSec * 1000)
    )
}
