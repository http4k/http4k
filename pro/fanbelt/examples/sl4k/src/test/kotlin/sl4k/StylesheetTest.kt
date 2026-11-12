package sl4k

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sl4k.web.viewerCookie

class StylesheetTest {
    private val cookie = viewerCookie(viewerNamed("ann"))
    private val app = ChatApp()

    private fun page(path: String) = Jsoup.parse(app.get(path, cookie).bodyString())

    @Test
    fun `the stylesheet is served as css`() {
        val response = app.http(Request(GET, "/public/main.css"))

        assertEquals(OK, response.status)
        assertEquals("text/css", response.header("content-type")?.substringBefore(";"))
        assertTrue(".message {" in response.bodyString())
    }

    @Test
    fun `every page links the stylesheet instead of carrying it`() {
        listOf("c/general", "search?q=hello").map(::page).forEach {
            assertEquals(listOf("/public/main.css"), it.select("link[rel=stylesheet][href^=/]").map { link -> link.attr("href") })
            assertFalse(it.select("style").any { style -> ".message {" in style.data() })
        }
    }
}
