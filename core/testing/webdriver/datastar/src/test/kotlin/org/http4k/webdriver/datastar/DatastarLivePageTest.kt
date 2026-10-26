package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.with
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.Selector
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.PipedInputStream
import java.io.PipedOutputStream

class DatastarLivePageTest {

    private fun patch(html: String, selector: String) =
        PatchElements(html, selector = Selector.of(selector)).toSseEvent().toMessage()

    @Test
    fun `data-init fires when the page loads`() {
        val home = """<html><body data-init="@get('/init')"><div id='greeting'>placeholder</div></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).body(home) },
            "/init" bind GET to { Response(OK).body(patch("<div id='greeting'>loaded</div>", "#greeting")) }
        )

        val driver = driverFor(app)
        driver.get("/")

        assertThat(driver.pageSource, containsSubstring("loaded"))
    }

    @Test
    fun `relative action urls resolve against the base href`() {
        val probes = mutableListOf<Request>()
        val home = """
            <html><head><base href="/w/acme/"></head><body>
                <button id='btn' data-on:click="@post('things/1/done')">go</button>
            </body></html>
        """.trimIndent()
        val app = routes(
            "/w/acme/page" bind GET to { Response(OK).body(home) },
            "/w/acme/things/1/done" bind POST to { probes.add(it); Response(OK) }
        )

        val driver = driverFor(app)
        driver.get("/w/acme/page")
        driver.findElement(By.id("btn")).click()

        assertThat(probes.map { it.uri.path }, equalTo(listOf("/w/acme/things/1/done")))
    }

    @Test
    fun `relative action urls resolve against the current page without a base`() {
        val probes = mutableListOf<Request>()
        val home = """<html><body><button id='btn' data-on:click="@post('done')">go</button></body></html>"""
        val app = routes(
            "/things/page" bind GET to { Response(OK).body(home) },
            "/things/done" bind POST to { probes.add(it); Response(OK) }
        )

        val driver = driverFor(app)
        driver.get("/things/page")
        driver.findElement(By.id("btn")).click()

        assertThat(probes.map { it.uri.path }, equalTo(listOf("/things/done")))
    }

    @Test
    fun `actions carry the browser's cookies and ask for an event stream`() {
        val probes = mutableListOf<Request>()
        val home = """<html><body><button id='btn' data-on:click="@post('/probe')">go</button></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).cookie(Cookie("viewer", "ann")).body(home) },
            "/probe" bind POST to { probes.add(it); Response(OK) }
        )

        val driver = driverFor(app)
        driver.get("/")
        driver.findElement(By.id("btn")).click()

        assertThat(probes.single().cookie("viewer")?.value, equalTo("ann"))
        assertThat(probes.single().header("Accept").orEmpty(), containsSubstring("text/event-stream"))
    }

    @Test
    fun `a streamed response keeps patching the page as events arrive`() {
        val events = PipedOutputStream()
        val stream = PipedInputStream(events)
        val home = """<html><body data-init="@get('/live')"><div id='count'>none</div></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).body(home) },
            "/live" bind GET to {
                events.write(patch("<div id='count'>one</div>", "#count").toByteArray())
                Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(stream)
            }
        )

        val driver = driverFor(app)
        driver.get("/")

        assertThat(driver.findElement(By.id("count")).text, equalTo("one"))

        events.write(patch("<div id='count'>two</div>", "#count").toByteArray())

        assertThat(driver.findElement(By.id("count")).text, equalTo("two"))
        assertThat(driver.pageSource, containsSubstring("two"))
    }

    @Test
    fun `settling applies events that have arrived on a live stream`() {
        val events = PipedOutputStream()
        val stream = PipedInputStream(events)
        val home = """<html><body data-init="@get('/live')"><div id='count'>none</div></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).body(home) },
            "/live" bind GET to { Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(stream) }
        )

        val driver = driverFor(app)
        driver.get("/")

        events.write(patch("<div id='count'>one</div>", "#count").toByteArray())
        driver.settle()

        assertThat(driver.findElement(By.id("count")).text, equalTo("one"))
    }

    @Test
    fun `without auto-settle, only settling applies live events and fires the actions they introduce`() {
        val probes = mutableListOf<Request>()
        val events = PipedOutputStream()
        val stream = PipedInputStream(events)
        val home = """<html><body data-init="@get('/live')"><div id='count'>none</div></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).body(home) },
            "/live" bind GET to { Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(stream) },
            "/probe" bind POST to { probes.add(it); Response(OK) }
        )

        val driver = DatastarWebDriver(app, autoSettle = false)
        driver.get("/")

        events.write(patch("<div id='count' data-init=\"@post('/probe')\">one</div>", "#count").toByteArray())

        assertThat(driver.findElement(By.id("count")).text, equalTo("none"))
        assertThat(driver.pageSource, containsSubstring("none"))
        assertThat(probes.size, equalTo(0))

        driver.settle()

        assertThat(driver.findElement(By.id("count")).text, equalTo("one"))
        assertThat(probes.size, equalTo(1))
    }

    @Test
    fun `navigating away closes a live stream`() {
        var closed = false
        val events = PipedOutputStream()
        val stream = object : PipedInputStream(events) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        val home = """<html><body data-init="@get('/live')"><div id='count'>none</div></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).body(home) },
            "/elsewhere" bind GET to { Response(OK).body("<html><body>elsewhere</body></html>") },
            "/live" bind GET to { Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(stream) }
        )

        val driver = driverFor(app)
        driver.get("/")
        driver.get("/elsewhere")

        assertThat(closed, equalTo(true))
    }

    @Test
    fun `closing the browser closes every live stream`() {
        val closed = mutableListOf<String>()
        fun liveStream(name: String) = object : PipedInputStream(PipedOutputStream()) {
            override fun close() {
                closed += name
                super.close()
            }
        }
        val home = """<html><body data-init="@get('/one'); @get('/two')"></body></html>"""
        val app = routes(
            "/" bind GET to { Response(OK).body(home) },
            "/one" bind GET to { Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(liveStream("one")) },
            "/two" bind GET to { Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(liveStream("two")) }
        )

        val driver = driverFor(app)
        driver.get("/")
        driver.close()
        driver.quit()

        assertThat(closed, equalTo(listOf("one", "two")))
    }
}
