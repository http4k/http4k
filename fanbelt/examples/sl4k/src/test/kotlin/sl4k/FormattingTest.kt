package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
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

        assertThat(text.selectFirst(".bold")?.text(), equalTo("very"))
        assertThat(text.text(), equalTo("this is very good"))
    }

    @Test
    fun `html in a message stays text`() {
        val text = rendered("<script>alert(1)</script> *<img src=x onerror=alert(1)>*")

        assertThat(text.select("script, img").size, equalTo(0))
        assertThat(text.selectFirst(".bold")?.text(), equalTo("<img src=x onerror=alert(1)>"))
    }

    @Test
    fun `underscores make a word italic and tildes strike it through`() {
        val text = rendered("_slowly_ and ~never~")

        assertThat(text.selectFirst(".italic")?.text(), equalTo("slowly"))
        assertThat(text.selectFirst(".strike")?.text(), equalTo("never"))
        assertThat(text.text(), equalTo("slowly and never"))
    }

    @Test
    fun `marks nest`() {
        val text = rendered("*bold _both_*")

        assertThat(text.selectFirst(".bold.italic")?.text(), equalTo("both"))
    }

    @Test
    fun `underscores and stars inside a word are left alone`() {
        val text = rendered("snake_case_name and 2*3*4")

        assertThat(text.select("span").size, equalTo(0))
        assertThat(text.text(), equalTo("snake_case_name and 2*3*4"))
    }

    @Test
    fun `nothing is formatted inside code`() {
        val text = rendered("run `*not bold*` now")

        assertThat(text.selectFirst("code")?.text(), equalTo("*not bold*"))
        assertThat(text.select(".bold").size, equalTo(0))
    }

    @Test
    fun `three backticks make a code block`() {
        val text = rendered("```val a = *1*```")

        assertThat(text.selectFirst("pre")?.text(), equalTo("val a = *1*"))
    }

    @Test
    fun `http and https addresses become links that leave our page safely`() {
        val link = rendered("see https://example.com/a_b_c, then").selectFirst("a")!!

        assertThat(link.attr("href"), equalTo("https://example.com/a_b_c"))
        assertThat(link.text(), equalTo("https://example.com/a_b_c"))
        assertThat(link.attr("target"), equalTo("_blank"))
        assertThat(link.attr("rel"), equalTo("noopener noreferrer"))
    }

    @Test
    fun `a javascript link stays text`() {
        val text = rendered("javascript:alert(1)")

        assertThat(text.select("a").size, equalTo(0))
        assertThat(text.text(), equalTo("javascript:alert(1)"))
    }

    @Test
    fun `a quote can't break out of a link`() {
        val text = rendered("""https://example.com/"onmouseover="alert(1)""")

        assertThat(text.select("[onmouseover]").size, equalTo(0))
    }
}
