package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import java.time.Clock

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
        var closed = 0
        val keys = mutableListOf<CharSequence>()

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

        override fun keysSent(element: Element, keys: List<CharSequence>) {
            this.keys += keys
        }

        override fun displayed(element: Element) = element.id() != "hidden"

        override fun closed() {
            closed++
        }
    }

    private val behaviour = RecordingBehaviour()
    private val driver = Http4kWebDriver(app, behaviour = behaviour)

    @Test
    fun `pageLoaded fires on get, link navigation and history moves`() {
        driver.get("/")
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
    fun `closed fires once when the open page is closed or quit`() {
        driver.get("/")
        driver.close()
        driver.close()
        assertThat(behaviour.closed, equalTo(1))

        driver.get("/")
        driver.quit()
        driver.quit()
        assertThat(behaviour.closed, equalTo(2))
    }

    @Test
    fun `the loaded document is live - mutations are visible to element lookups and morphs persist`() {
        driver.get("/")
        behaviour.loaded.last().selectFirst("#name")?.attr("value", "morphed")

        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("morphed"))
    }

    @Test
    fun `events fire around default interactions`() {
        driver.get("/")
        driver.findElement(By.id("name")).sendKeys("typed")

        assertThat(
            behaviour.events,
            equalTo(listOf("before #name" to PageEvent.SendKeys, "after #name" to PageEvent.SendKeys))
        )
        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("typed"))
    }

    @Test
    fun `sent keys reach the behaviour but special keys do not reach the value`() {
        driver.get("/")
        driver.findElement(By.id("name")).sendKeys("typed", Keys.ENTER)

        assertThat(behaviour.keys, equalTo(listOf<CharSequence>("typed", Keys.ENTER)))
        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("typed"))
    }

    @Test
    fun `a handled beforeEvent suppresses the default interaction and afterEvent`() {
        driver.get("/")
        behaviour.handleEvents = true

        driver.findElement(By.id("link")).click()

        assertThat(driver.currentUrl!!, containsSubstring("/")) // no navigation to /two
        assertThat(behaviour.loaded.size, equalTo(1))
        assertThat(behaviour.events, equalTo(listOf("before #link" to PageEvent.Click)))
    }

    @Test
    fun `displayed is delegated to the behaviour`() {
        driver.get("/")

        assertThat(driver.findElement(By.id("hidden")).isDisplayed, equalTo(false))
        assertThat(driver.findElement(By.id("name")).isDisplayed, equalTo(true))
    }

    @Test
    fun `a behaviour built from the browser's handler sends cookies and resolves urls against the page and its base`() {
        val seen = mutableListOf<Request>()
        val site = routes(
            "/w/acme/page" bind GET to {
                Response(OK).cookie(Cookie("viewer", "ann"))
                    .body("""<html><head><base href="/w/acme/"></head><body><div id='inner'><span id='leaf'>x</span></div></body></html>""")
            },
            "/w/acme/things/done" bind GET to { seen.add(it); Response(OK) }
        )
        var finds = 0
        val browser = Http4kWebDriver(site, Clock.systemUTC()) { handler ->
            object : PageBehaviour {
                override fun beforeFind() {
                    finds++
                }

                override fun pageLoaded(document: Document) {
                    handler(Request(GET, "things/done"))
                }
            }
        }

        browser.get("/w/acme/page")
        browser.findElement(By.id("inner")).findElement(By.id("leaf"))

        assertThat(seen.single().uri.path, equalTo("/w/acme/things/done"))
        assertThat(seen.single().cookie("viewer")?.value, equalTo("ann"))
        assertThat(finds, equalTo(2))
    }

    @Test
    fun `without a behaviour nothing changes`() {
        val plain = Http4kWebDriver(app)
        plain.get("http://localhost/")
        plain.findElement(By.id("link")).click()

        assertThat(plain.currentUrl!!, containsSubstring("/two"))
    }

    @Test
    fun `a find sees the page the behaviour navigated to while catching up`() {
        val site = routes(
            "/first" bind GET to { Response(OK).body("<html><body><p id='where'>first</p></body></html>") },
            "/second" bind GET to { Response(OK).body("<html><body><p id='where'>second</p></body></html>") }
        )
        lateinit var browser: Http4kWebDriver
        browser = Http4kWebDriver(site, Clock.systemUTC()) {
            object : PageBehaviour {
                override fun beforeFind() {
                    if (browser.currentUrl.orEmpty().endsWith("/first")) browser.get("/second")
                }
            }
        }

        browser.get("/first")

        assertThat(browser.findElement(By.id("where")).text, equalTo("second"))
    }
}
