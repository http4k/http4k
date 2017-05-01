package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.filters.ResponseFilters.ReportLatency
import org.reekwest.http.toHttpHandler
import org.reekwest.http.util.TickingClock
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