package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import sl4k.web.viewerCookie

class SearchPageTest {
    private val cookie = viewerCookie(viewerNamed("ann"))
    private val app = ChatApp()

    private fun post(path: String, body: String) = app.post(path, cookie, body)

    private fun search(query: String) = Jsoup.parse(app.http(Request(GET, "/teams/acme/search").query("q", query).cookie(cookie)).bodyString())

    @Test
    fun `a result opens the thread it's in`() {
        post("c/general/messages", """{"text":"Lunch?"}""")
        val lunch = app.storage.chat { it.messages.page().items.single().id }
        post("c/general/t/$lunch/replies", """{"reply":"Tacos"}""")

        assertThat(search("tacos").selectFirst("#results .result a.channel")?.attr("href"), equalTo("c/general/t/$lunch"))
        assertThat(search("lunch").selectFirst("#results .result a.channel")?.attr("href"), equalTo("c/general/t/$lunch"))
    }

    @Test
    fun `a search can't inject anything into the page, and stays in the box`() {
        val hostile = "<script>alert(1)</script>"
        val page = search(hostile)

        assertThat(page.select("#results script").size, equalTo(0))
        assertThat(page.selectFirst("form.search input")?.attr("value"), equalTo(hostile))
    }

    @Test
    fun `results stop at fifty`() {
        repeat(51) { post("c/general/messages", """{"text":"Lunch $it"}""") }

        assertThat(search("lunch").select("#results .result").size, equalTo(50))
    }
}
