package org.http4k.storyboard.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.Selector
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.sse.SseMessage
import org.http4k.storyboard.RecordingWebDriver
import org.http4k.storyboard.Storyboard
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.openqa.selenium.By
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

class DatastarStoryboardTest {

    companion object {
        private fun patch(html: String, selector: String): SseMessage.Event =
            PatchElements(html, selector = Selector.of(selector)).toSseEvent()

        private val home = """
            <html><body data-on-load="@get('/load')">
                <h1>Storyboard demo</h1>
                <div id='banner'>placeholder</div>
                <button id='go' data-on-click="@get('/click')">go</button>
                <div id='counter'>0</div>
            </body></html>
        """.trimIndent()

        private val counter = AtomicInteger(0)

        private val app: HttpHandler = routes(
            "/" bind GET to {
                counter.set(0)
                Response(OK).body(home)
            },
            "/load" bind GET to {
                Response(OK).body(sseBody(patch("<div id='banner'>loaded on load</div>", "#banner")))
            },
            "/click" bind GET to {
                val n = counter.incrementAndGet()
                Response(OK).body(sseBody(patch("<div id='counter'>$n</div>", "#counter")))
            }
        )
    }

    @JvmField
    @RegisterExtension
    val storyboard = Storyboard(
        http = app,
        driverFactory = { handler, clock -> DatastarWebDriver(Http4kWebDriver(handler, clock), handler) }
    )

    @Test
    fun `data-on-load and two clicks each increment a counter via data-on-click`(driver: RecordingWebDriver) {
        driver.get("http://localhost/")
        driver.capture("Initial", "after data-on-load fired (counter = 0)")

        driver.findElement(By.id("go")).click()
        driver.capture("After click 1", "counter incremented to 1")

        driver.findElement(By.id("go")).click()
        driver.capture("After click 2", "counter incremented to 2")

        val titles = driver.frames().map { it.title }
        assertThat(
            titles,
            equalTo(
                listOf(
                    "Initial",
                    "click [By.id: go]",
                    "After click 1",
                    "click [By.id: go]",
                    "After click 2"
                )
            )
        )

        val decoder = Base64.getDecoder()
        fun dom(idx: Int) = String(decoder.decode(driver.frames()[idx].dom))

        assertThat(dom(0), containsSubstring("loaded on load"))
        assertThat(dom(0), containsSubstring("<div id=\"counter\">0</div>"))
        assertThat(dom(2), containsSubstring("<div id=\"counter\">1</div>"))
        assertThat(dom(4), containsSubstring("<div id=\"counter\">2</div>"))
    }
}
