package features.builds

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
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Test

class BuildsTest {
    private val http = App(TestRuntime()).apply { start() }.handler.http!!
    private val ci = ClientFilters.BearerAuth("local-ci").then(http)

    init {
        http(Request(POST, "/spaces").form("space", "acme"))
    }

    private fun report(status: String, commit: String) =
        Request(POST, "/spaces/acme/builds").header("content-type", "application/json").body("""{"status":"$status","commit":"$commit"}""")

    private fun page() = http(Request(GET, "/spaces/acme/builds").cookie(viewerCookie(viewerNamed("ann")))).bodyString()

    @Test
    fun `a build reported by CI shows on the builds page`() {
        assertThat(ci(report("passing", "abc1234")).status, equalTo(ACCEPTED))

        assertThat(page(), containsSubstring("abc1234"))
    }

    @Test
    fun `a report that makes no sense is refused with the reason`() {
        val response = ci(report("exploded", "abc1234"))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("A build is passing, failing or running"))
    }

    @Test
    fun `only CI, with its token, can report builds`() {
        val guessing = ClientFilters.BearerAuth("guessed").then(http)

        assertThat(guessing(report("passing", "abc1234")).status, equalTo(UNAUTHORIZED))
    }
}
