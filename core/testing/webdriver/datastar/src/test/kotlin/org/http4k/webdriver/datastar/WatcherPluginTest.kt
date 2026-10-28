package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.PipedInputStream
import java.io.PipedOutputStream

class WatcherPluginTest {

    private fun patch(html: String, selector: String, mode: MorphMode = MorphMode.outer) =
        PatchElements(html, morphMode = mode, selector = Selector.of(selector)).toSseEvent()

    private val followsLinks = WatcherPlugin { element, page ->
        if (element.tagName() == "a" && element.hasClass("follow")) page.navigate(element.attr("href"))
    }

    @Test
    fun `an extension is told about every element a patch carries`() {
        val seen = mutableListOf<String>()
        val app = routes(
            "/" bind GET to { Response(OK).body("""<html><body><button id='btn' data-on:click="@post('/go')">go</button><ul id='list'></ul></body></html>""") },
            "/go" bind POST to { Response(OK).body(sseBody(patch("<li>one</li><li>two</li>", "#list", MorphMode.append))) }
        )

        val driver = DatastarWebDriver(app, plugins = listOf(WatcherPlugin { element, _ -> seen += element.text() }))
        driver.get("/")
        driver.findElement(By.id("btn")).click()

        assertThat(seen, equalTo(listOf("one", "two")))
        assertThat(driver.pageSource, containsSubstring("two"))
    }

    @Test
    fun `an extension can navigate, resolving against the page's base once the action has been applied`() {
        val app = routes(
            "/w/acme/" bind GET to {
                Response(OK).body("""<html><head><base href="/w/acme/"></head><body><button id='btn' data-on:click="@post('go')">go</button><div id='out'></div></body></html>""")
            },
            "/w/acme/go" bind POST to {
                Response(OK).body(sseBody(patch("<a class='follow' href='things/1'>next</a>", "#out", MorphMode.inner), patch("<div id='out'>done</div>", "#out")))
            },
            "/w/acme/things/1" bind GET to { Response(OK).body("<html><body><p id='arrived'>thing 1</p></body></html>") }
        )

        val driver = DatastarWebDriver(app, plugins = listOf(followsLinks))
        driver.get("http://localhost/w/acme/")
        driver.findElement(By.id("btn")).click()

        assertThat(driver.currentUrl.orEmpty(), endsWith("/w/acme/things/1"))
        assertThat(driver.findElement(By.id("arrived")).text, equalTo("thing 1"))
    }

    @Test
    fun `an extension can navigate from a patch that arrives on a live stream`() {
        val output = PipedOutputStream()
        val stream = PipedInputStream(output)
        val app = routes(
            "/" bind GET to { Response(OK).body("""<html><body data-init="@get('/live')"><div id='out'></div></body></html>""") },
            "/live" bind GET to { Response(OK).with(CONTENT_TYPE of TEXT_EVENT_STREAM).body(stream) },
            "/next" bind GET to { Response(OK).body("<html><body><p id='arrived'>next</p></body></html>") }
        )

        val driver = DatastarWebDriver(app, plugins = listOf(followsLinks))
        driver.get("/")
        output.write(patch("<a class='follow' href='/next'>next</a>", "#out", MorphMode.inner).toMessage().toByteArray())
        output.flush()

        assertThat(driver.findElement(By.id("arrived")).text, equalTo("next"))
    }

    @Test
    fun `without extensions, patched elements are only morphed in`() {
        val app = routes(
            "/" bind GET to { Response(OK).body("""<html><body><button id='btn' data-on:click="@post('/go')">go</button><div id='out'></div></body></html>""") },
            "/go" bind POST to { Response(OK).body(sseBody(patch("<a class='follow' href='/elsewhere'>next</a>", "#out", MorphMode.inner))) }
        )

        val driver = driverFor(app)
        driver.get("/")
        driver.findElement(By.id("btn")).click()

        assertThat(driver.currentUrl.orEmpty(), endsWith("/"))
        assertThat(driver.pageSource, containsSubstring("class=\"follow\""))
    }
}
