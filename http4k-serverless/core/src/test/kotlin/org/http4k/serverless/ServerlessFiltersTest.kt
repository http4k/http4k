package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.filter.ZipkinTraces
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class ServerlessFiltersTest {
    val request = "999"
    val response = 999

    @Test
    fun `reporting latency for function`() {
        var called = false

        ServerlessFilters.ReportFnTransaction<String, String, Int>(TickingClock()) { (req, resp, duration) ->
            called = true
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(Duration.ofSeconds(1)))
        }.then { `in`: String, _: String -> `in`.toInt() }(request, "")

        assertTrue(called)
    }

    @Test
    fun `request tracing when set`() {
        val storage = ZipkinTracesStorage.THREAD_LOCAL
        val before = storage.forCurrentThread()
        ServerlessFilters.RequestTracing<String, String, Int>(storage = storage)
            .then { `in`: String, _: String ->
                assertThat(storage.forCurrentThread(), equalTo(before))
                `in`.toInt()
            }(request, "")

        assertThat(storage.forCurrentThread(), equalTo(before))
    }

    @Test
    fun `request tracing when none`() {
        val storage = ZipkinTracesStorage.THREAD_LOCAL
        var set: ZipkinTraces? = null
        ServerlessFilters.RequestTracing<String, String, Int>(storage = storage)
            .then { `in`: String, _: String ->
                set = storage.forCurrentThread()
                assertThat(storage.forCurrentThread(), present())
                `in`.toInt()
            }(request, "")

        assertThat(storage.forCurrentThread(), !equalTo(set))
    }
}
