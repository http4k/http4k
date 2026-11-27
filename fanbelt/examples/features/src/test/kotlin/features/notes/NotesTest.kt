package features.notes

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import features.App
import features.viewerCookie
import features.viewerNamed
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test

class NotesTest {
    private val tenant = TestTenant()
    private val notes = NotesFeature(tenant.runtime.random)
    private val ann = viewerNamed("ann")

    private fun posts(count: Int) = (1..count).forEach { n -> tenant { given(notes.post, ann, "note $n") } }

    @Test
    fun `a window shows the newest notes first, and whether there are more`() {
        posts(25)

        val window = tenant { read(notes.newest, Shown.of(20)) }

        assertThat(window.notes.map { it.text.value }, equalTo((25 downTo 6).map { "note $it" }))
        assertThat(window.more, equalTo(true))
    }

    @Test
    fun `a note posted while a window is open shows in it`() {
        tenant { given(notes.post, ann, "first") }
        tenant { read(notes.newest, Shown.of(20)) }

        tenant { given(notes.post, ann, "second") }

        assertThat(tenant { read(notes.newest, Shown.of(20)) }.notes.map { it.text.value }, equalTo(listOf("second", "first")))
    }

    @Test
    fun `an empty note is refused`() {
        assertThat(tenant { execute(notes.post, ann, " ") }, equalTo(Verdict.Rejected("A note is 1 to 280 characters")))
    }

    @Test
    fun `show more is a link that widens the window`() {
        val http = App(TestRuntime()).apply { start() }.handler.http!!
        val cookie = viewerCookie(ann)
        http(Request(POST, "/spaces").form("space", "acme"))
        repeat(25) { http(Request(POST, "/spaces/acme/notes").cookie(cookie).header("content-type", "application/json").body("""{"note":"note $it"}""")) }

        val first = http(Request(GET, "/spaces/acme/notes").cookie(cookie)).bodyString()
        val wider = http(Request(GET, "/spaces/acme/notes?show=40").cookie(cookie)).bodyString()

        assertThat(first, containsSubstring("""href="notes?show=40""""))
        assertThat(Regex("<li>").findAll(first).count(), equalTo(20))
        assertThat(Regex("<li>").findAll(wider).count(), equalTo(25))
    }
}
