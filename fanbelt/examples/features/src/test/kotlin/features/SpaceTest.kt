package features

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test

class SpaceTest {
    private val http = App(TestRuntime()).apply { start() }.handler.http!!
    private val ann = viewerCookie(viewerNamed("ann"))

    init {
        http(Request(POST, "/spaces").form("space", "acme"))
        http(Request(POST, "/spaces/acme/rooms").cookie(ann).header("content-type", "application/json").body("""{"name":"plans","private":false}"""))
    }

    private fun page(path: String) = http(Request(GET, "/spaces/acme/$path").cookie(ann)).bodyString()

    private val everyTool = allOf(listOf("plan", "rooms", "notes", "builds", "status").map { containsSubstring("""href="$it"""") })

    @Test
    fun `the space opens on an overview of the team's tools`() {
        assertThat(page(""), allOf(containsSubstring("<h1>acme</h1>"), everyTool))
    }

    @Test
    fun `the planning sheet is at plan`() {
        assertThat(page("plan"), containsSubstring("""id="grid""""))
    }

    @Test
    fun `every page of the space carries the team's name and the way to each tool`() {
        listOf("plan", "rooms", "rooms/plans", "notes", "builds", "status").forEach {
            assertThat(it, page(it), allOf(containsSubstring("""<a href="" class="team">acme</a>"""), everyTool))
        }
    }
}
