package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class PageBehaviourTest {

    private val app = routes(
        "/" bind GET to {
            Response(OK).body(
                """<html><body>
                    <a id='link' href='/two'>away</a>
                    <input id='name' value='before'/>
                    <div id='hidden'>secret</div>
                </body></html>"""
            )
        },
        "/two" bind GET to { Response(OK).body("<html><body><h1>two</h1></body></html>") }
    )

    private class RecordingBehaviour : PageBehaviour {
        val loaded = mutableListOf<Document>()
        val events = mutableListOf<Pair<String, PageEvent>>()
        var handleEvents = false

        override fun pageLoaded(document: Document) {
            loaded.add(document)
        }

        override fun before(event: PageEvent, element: Element): Boolean {
            events.add("before #${element.id()}" to event)
            return handleEvents
        }

        override fun after(event: PageEvent, element: Element) {
            events.add("after #${element.id()}" to event)
        }

        override fun displayed(element: Element) = element.id() != "hidden"
    }

    private val behaviour = RecordingBehaviour()
    private val driver = Http4kWebDriver(app, behaviour = behaviour)

    @Test
    fun `pageLoaded fires on get, link navigation and history moves`() {
        driver.get("http://localhost/")
        assertThat(behaviour.loaded.size, equalTo(1))

        driver.findElement(By.id("link")).click()
        assertThat(behaviour.loaded.size, equalTo(2))
        assertThat(behaviour.loaded.last().select("h1").text(), equalTo("two"))

        driver.navigate().back()
        assertThat(behaviour.loaded.size, equalTo(3))

        driver.navigate().forward()
        assertThat(behaviour.loaded.size, equalTo(4))
    }

    @Test
    fun `the loaded document is live - mutations are visible to element lookups and morphs persist`() {
        driver.get("http://localhost/")
        behaviour.loaded.last().selectFirst("#name")?.attr("value", "morphed")

        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("morphed"))
    }

    @Test
    fun `events fire around default interactions`() {
        driver.get("http://localhost/")
        driver.findElement(By.id("name")).sendKeys("typed")

        assertThat(
            behaviour.events,
            equalTo(listOf("before #name" to PageEvent.SendKeys, "after #name" to PageEvent.SendKeys))
        )
        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("typed"))
    }

    @Test
    fun `a handled beforeEvent suppresses the default interaction and afterEvent`() {
        driver.get("http://localhost/")
        behaviour.handleEvents = true

        driver.findElement(By.id("link")).click()

        assertThat(driver.currentUrl!!, containsSubstring("/")) // no navigation to /two
        assertThat(behaviour.loaded.size, equalTo(1))
        assertThat(behaviour.events, equalTo(listOf("before #link" to PageEvent.Click)))
    }

    @Test
    fun `displayed is delegated to the behaviour`() {
        driver.get("http://localhost/")

        assertThat(driver.findElement(By.id("hidden")).isDisplayed, equalTo(false))
        assertThat(driver.findElement(By.id("name")).isDisplayed, equalTo(true))
    }

    @Test
    fun `without a behaviour nothing changes`() {
        val plain = Http4kWebDriver(app)
        plain.get("http://localhost/")
        plain.findElement(By.id("link")).click()

        assertThat(plain.currentUrl!!, containsSubstring("/two"))
    }
}
