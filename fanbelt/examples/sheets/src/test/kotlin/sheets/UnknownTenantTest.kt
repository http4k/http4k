package sheets

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test
import sheets.web.viewerCookie
import java.time.Instant.ofEpochMilli

class UnknownTenantTest {
    private val scheduler = DeterministicScheduler(ofEpochMilli(0))
    private val app = App(TestRuntime(scheduler)).handler
    private val http = app.http!!

    init {
        http(Request(POST, "/sheets").form("sheet", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, and one created through the home page is`() {
        assertThat(http(Request(GET, "/sheets/demo/")).status, !equalTo(NOT_FOUND))
        assertThat(http(Request(GET, "/sheets/ghost/")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `nor is its live stream`() {
        val sse = app.sse!!
        val viewer = viewerCookie(viewerNamed("ann"))

        assertThat(sse(Request(GET, "/sheets/demo/").cookie(viewer)).status, !equalTo(NOT_FOUND))
        assertThat(sse(Request(GET, "/sheets/ghost/").cookie(viewer)).status, equalTo(NOT_FOUND))
    }
}
