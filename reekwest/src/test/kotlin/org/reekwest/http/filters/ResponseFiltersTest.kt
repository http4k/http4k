package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.filters.ResponseFilters.ReportLatency
import org.reekwest.http.util.TickingClock
import java.time.Duration

class ResponseFiltersTest {

    @Test
    fun `reporting latency for request`() {
        var called: Triple<Request, Response, Duration>? = null
        val request = get("")
        val response = Response(OK)

        val handler = ReportLatency(TickingClock, { req, resp, duration -> called = Triple(req, resp, duration) }).then { response }

        handler(request)

        assertThat(called, equalTo(Triple(request, response, Duration.ofSeconds(1))))
    }
}