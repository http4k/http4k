package helpdesk

import dev.forkhandles.time.DeterministicScheduler
import helpdesk.fakes.FakeEmailProvider
import helpdesk.web.viewerCookie
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

class UnknownTenantTest {
    private val scheduler = DeterministicScheduler(start)
    private val http = App(TestRuntime(scheduler), provider = FakeEmailProvider()).handler.http!!

    init {
        http(Request(POST, "/orgs").form("org", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, for its pages and its commands`() {
        assertNotEquals(NOT_FOUND, http(Request(GET, "/orgs/demo/")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/orgs/ghost/")).status)
        assertEquals(NOT_FOUND, http(Request(POST, "/orgs/ghost/t/00000000-0000-0000-0000-000000000000/assign")).status)
        assertEquals(NOT_FOUND, http(Request(GET, "/orgs/not%20a%20name/")).status)
    }

    @Test
    fun `a ticket that doesn't exist is 404, with the page's gone header`() {
        val response = http(Request(GET, "/orgs/demo/t/00000000-0000-0000-0000-000000000001").cookie(viewerCookie(viewerNamed("ann"))))

        assertEquals(NOT_FOUND, response.status)
        assertEquals(true, response.bodyString().contains("This ticket no longer exists"))
    }
}