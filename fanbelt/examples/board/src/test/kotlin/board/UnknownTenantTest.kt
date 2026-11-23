package board

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.body.form
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.start
import org.junit.jupiter.api.Test

class UnknownTenantTest {
    private val scheduler = DeterministicScheduler(start)
    private val http = App(TestRuntime(scheduler)).handler.http!!

    init {
        http(Request(POST, "/teams").form("team", "demo"))
    }

    @Test
    fun `a tenant that was never created is not found, for its pages and its commands`() {
        assertThat(http(Request(GET, "/teams/demo/")).status, !equalTo(NOT_FOUND))
        assertThat(http(Request(GET, "/teams/ghost/")).status, equalTo(NOT_FOUND))
        assertThat(http(Request(POST, "/teams/ghost/b/lobby/join")).status, equalTo(NOT_FOUND))
        assertThat(http(Request(GET, "/teams/not%20a%20name/")).status, equalTo(NOT_FOUND))
    }
}