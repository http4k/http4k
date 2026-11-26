package features.rooms

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import features.App
import features.viewerCookie
import features.viewerNamed
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test

class RoomPagesTest {
    private val http = App(TestRuntime()).apply { start() }.handler.http!!
    private val ann = viewerCookie(viewerNamed("ann"))
    private val bob = viewerCookie(viewerNamed("bob"))

    init {
        http(Request(POST, "/spaces").form("space", "acme"))
    }

    private fun post(who: Cookie, path: String, signals: String) =
        http(Request(POST, "/spaces/acme/$path").cookie(who).header("content-type", "application/json").body(signals))

    private fun page(who: Cookie, path: String) = http(Request(GET, "/spaces/acme/$path").cookie(who))

    @Test
    fun `creating a room takes you into it`() {
        assertThat(post(ann, "rooms", """{"name":"plans","private":false}""").bodyString(), containsSubstring("rooms/plans"))
    }

    @Test
    fun `a private room is listed for, and opens for, its owner alone`() {
        post(ann, "rooms", """{"name":"plans","private":true}""")

        assertThat(page(ann, "rooms").bodyString(), containsSubstring("""href="rooms/plans""""))
        assertThat(page(bob, "rooms").bodyString().contains("rooms/plans"), equalTo(false))
        assertThat(page(bob, "rooms/plans").status, equalTo(NOT_FOUND))
    }

    @Test
    fun `a room shows how much is new since you were last in it`() {
        post(ann, "rooms", """{"name":"plans","private":false}""")
        post(ann, "rooms/plans/say", """{"line":"one"}""")
        post(ann, "rooms/plans/say", """{"line":"two"}""")

        assertThat(page(bob, "rooms/plans").bodyString(), containsSubstring("2 new since you were last here"))
    }

    @Test
    fun `only the owner is offered the rename`() {
        post(ann, "rooms", """{"name":"plans","private":false}""")

        assertThat(page(ann, "rooms/plans").bodyString(), containsSubstring("rooms/plans/rename"))
        assertThat(page(bob, "rooms/plans").bodyString(), !containsSubstring("rooms/plans/rename"))
    }
}
