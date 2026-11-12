package todo

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.body.form
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.start
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UnknownTenantTest {
    private val scheduler = DeterministicScheduler(start)
    private val app = App(TestRuntime(scheduler)).handler
    private val http = app.http!!

    init {
        http(Request(POST, "/lists").form("list", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, for its pages and its commands`() {
        assertNotEquals(NOT_FOUND, http(Request(GET, "/lists/demo/")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/lists/ghost/")).status)
        assertEquals(NOT_FOUND, http(Request(POST, "/lists/ghost/todos")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/lists/not%20a%20name/")).status)
    }

    @Test
    fun `nor is its live stream`() {
        val sse = app.sse!!

        assertNotEquals(NOT_FOUND, sse(Request(GET, "/lists/demo/")).status)
        assertEquals(NOT_FOUND, sse(Request(GET, "/lists/ghost/")).status)
    }
}