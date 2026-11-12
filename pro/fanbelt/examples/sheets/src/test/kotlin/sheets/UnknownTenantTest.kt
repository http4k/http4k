package sheets

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.start
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import sheets.web.viewerCookie

class UnknownTenantTest {
    private val scheduler = DeterministicScheduler(start)
    private val app = App(TestRuntime(scheduler)).handler
    private val http = app.http!!

    init {
        http(Request(POST, "/sheets").form("sheet", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, and one created through the home page is`() {
        assertNotEquals(NOT_FOUND, http(Request(GET, "/sheets/demo/")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/sheets/ghost/")).status)
    }

    @Test
    fun `nor is its live stream`() {
        val sse = app.sse!!
        val viewer = viewerCookie(viewerNamed("ann"))

        assertNotEquals(NOT_FOUND, sse(Request(GET, "/sheets/demo/").cookie(viewer)).status)
        assertEquals(NOT_FOUND, sse(Request(GET, "/sheets/ghost/").cookie(viewer)).status)
    }
}
