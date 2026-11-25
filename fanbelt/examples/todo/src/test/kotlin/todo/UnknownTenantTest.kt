package todo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.body.form
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test
import java.time.Instant.ofEpochMilli

class UnknownTenantTest {
    private val scheduler = DeterministicScheduler(ofEpochMilli(0))
    private val app = App(TestRuntime(scheduler)).handler
    private val http = app.http!!

    init {
        http(Request(POST, "/lists").form("list", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, for its pages and its commands`() {
        assertThat(http(Request(GET, "/lists/demo/")).status, !equalTo(NOT_FOUND))
        assertThat(http(Request(GET, "/lists/ghost/")).status, equalTo(NOT_FOUND))
        assertThat(http(Request(POST, "/lists/ghost/todos")).status, equalTo(NOT_FOUND))
        assertThat(http(Request(GET, "/lists/not%20a%20name/")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `nor is its live stream`() {
        val sse = app.sse!!

        assertThat(sse(Request(GET, "/lists/demo/")).status, !equalTo(NOT_FOUND))
        assertThat(sse(Request(GET, "/lists/ghost/")).status, equalTo(NOT_FOUND))
    }
}
