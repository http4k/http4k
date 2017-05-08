package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import junit.framework.TestCase.assertTrue
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResponseFilters.ReportLatency
import org.http4k.toHttpHandler
import org.http4k.util.TickingClock
import org.junit.Test
import java.time.Duration

class ResponseFiltersTest {

    @Test
    fun `tap passes response through to function`() {
        var called = false
        val response = Response(OK)
        ResponseFilters.Tap { called = true; assertThat(it, equalTo(response)) }.then(response.toHttpHandler())(get(""))
        assertTrue(called)
    }

    @Test
    fun `reporting latency for request`() {
        var called = false
        val request = get("")
        val response = Response(OK)

        ReportLatency(TickingClock, { req, resp, duration ->
            called = true;
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(Duration.ofSeconds(1)))
        }).then { response }(request)

        assertTrue(called)
    }
}