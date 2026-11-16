package sl4k

import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import sl4k.web.viewerCookie

class StylesheetTest {
    private val cookie = viewerCookie(viewerNamed("ann"))
    private val app = ChatApp()

    private fun page(path: String) = Jsoup.parse(app.get(path, cookie).bodyString())

    @Test
    fun `the stylesheet is served as css`() {
        val response = app.http(Request(GET, "/public/main.css"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("content-type")?.substringBefore(";"), equalTo("text/css"))
        assertThat(response.bodyString(), containsSubstring(".message {"))
    }

    @Test
    fun `every page links the stylesheet instead of carrying it`() {
        listOf("c/general", "search?q=hello").map(::page).forEach {
            assertThat(it.select("link[rel=stylesheet][href^=/]").map { link -> link.attr("href") }, equalTo(listOf("/public/main.css")))
            assertThat(it.select("style").map { style -> style.data() }, !anyElement(containsSubstring(".message {")))
        }
    }
}
