package sl4k

import org.http4k.core.cookie.cookie
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.web.viewerCookie

class FormattingTest {
    private fun rendered(text: String): Element {
        val app = ChatApp()
        val cookie = viewerCookie(viewerNamed("ann"))
        val body = """{"text":"${text.replace("\\", "\\\\").replace("\"", "\\\"")}"}"""
        app.post("c/general/messages", cookie, body)
        val page = app.get("c/general", cookie).bodyString()
        return Jsoup.parse(page).selectFirst("#history .text")!!
    }

    @Test
    fun `stars make a word bold, and the stars go`() {
        val text = rendered("this is *very* good")

        assertEquals("very", text.selectFirst(".bold")?.text())
        assertEquals("this is very good", text.text())
    }

    @Test
    fun `html in a message stays text`() {
        val text = rendered("<script>alert(1)</script> *<img src=x onerror=alert(1)>*")

        assertEquals(0, text.select("script, img").size)
        assertEquals("<img src=x onerror=alert(1)>", text.selectFirst(".bold")?.text())
    }

    @Test
    fun `underscores make a word italic and tildes strike it through`() {
        val text = rendered("_slowly_ and ~never~")

        assertEquals("slowly", text.selectFirst(".italic")?.text())
        assertEquals("never", text.selectFirst(".strike")?.text())
        assertEquals("slowly and never", text.text())
    }

    @Test
    fun `marks nest`() {
        val text = rendered("*bold _both_*")

        assertEquals("both", text.selectFirst(".bold.italic")?.text())
    }

    @Test
    fun `underscores and stars inside a word are left alone`() {
        val text = rendered("snake_case_name and 2*3*4")

        assertEquals(0, text.select("span").size)
        assertEquals("snake_case_name and 2*3*4", text.text())
    }

    @Test
    fun `nothing is formatted inside code`() {
        val text = rendered("run `*not bold*` now")

        assertEquals("*not bold*", text.selectFirst("code")?.text())
        assertEquals(0, text.select(".bold").size)
    }

    @Test
    fun `three backticks make a code block`() {
        val text = rendered("```val a = *1*```")

        assertEquals("val a = *1*", text.selectFirst("pre")?.text())
    }

    @Test
    fun `http and https addresses become links that leave our page safely`() {
        val link = rendered("see https://example.com/a_b_c, then").selectFirst("a")!!

        assertEquals("https://example.com/a_b_c", link.attr("href"))
        assertEquals("https://example.com/a_b_c", link.text())
        assertEquals("_blank", link.attr("target"))
        assertEquals("noopener noreferrer", link.attr("rel"))
    }

    @Test
    fun `a javascript link stays text`() {
        val text = rendered("javascript:alert(1)")

        assertEquals(0, text.select("a").size)
        assertEquals("javascript:alert(1)", text.text())
    }

    @Test
    fun `a quote can't break out of a link`() {
        val text = rendered("""https://example.com/"onmouseover="alert(1)""")

        assertEquals(0, text.select("[onmouseover]").size)
    }
}
