package sl4k

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.web.viewerCookie
import java.time.Duration

class HistoryPageTest {
    private val ann = viewerCookie(viewerNamed("ann"))
    private val app = ChatApp()

    private fun says(text: String) = app.post("c/general/messages", ann, """{"text":"$text"}""")

    @Test
    fun `the history is grouped by day`() {
        says("One")
        says("Two")
        app.scheduler.tick(Duration.ofDays(1))
        says("Three")

        val page = Jsoup.parse(app.get("c/general", ann).bodyString())

        assertEquals(listOf("Three", "Fri 2 Jan 1970", "Two", "One", "Thu 1 Jan 1970"), page.select("#history .text, #history .day").map { it.text() })
    }
}
