package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class ServerlessFiltersTest {
    @Test
    fun `reporting latency for function`() {
        var called = false

        val request = "999"
        val response = 999

        ServerlessFilters.ReportFnTransaction<String, String, Int>(TickingClock()) { (req, resp, duration) ->
            called = true
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(Duration.ofSeconds(1)))
        }.then { `in`: String, _: String -> `in`.toInt() }(request, "")

        assertTrue(called)
    }
}
