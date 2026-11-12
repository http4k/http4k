package board

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
    private val http = App(TestRuntime(scheduler)).handler.http!!

    init {
        http(Request(POST, "/teams").form("team", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, for its pages and its commands`() {
        assertNotEquals(NOT_FOUND, http(Request(GET, "/teams/demo/")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/teams/ghost/")).status)
        assertEquals(NOT_FOUND, http(Request(POST, "/teams/ghost/b/lobby/join")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/teams/not%20a%20name/")).status)
    }
}