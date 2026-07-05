package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarWebDriverTest {

    private fun patch(html: String, selector: String, mode: MorphMode = MorphMode.outer): SseMessage.Event =
        PatchElements(html, morphMode = mode, selector = Selector.of(selector)).toSseEvent()

    @Test
    fun `data-on-click triggers the action and morphs the DOM`() {
        val home = """
            <html><body>
                <button id='btn' data-on:click="@get('/clicked')">go</button>
                <div id='out'>before</div>
            </body></html>
        """.trimIndent()

        val app = routes(
            "/" bind Method.GET to { Response(OK).body(home) },
            "/clicked" bind Method.GET to {
                Response(OK).body(sseBody(patch("<div id='out'>after</div>", "#out")))
            }
        )

        val driver = driverFor(app)
        driver.get("/")

        driver.findElement(By.id("btn")).click()

        assertThat(driver.pageSource, containsSubstring("after"))
        assertThat(driver.pageSource, !containsSubstring("before"))
    }

    @Test
    fun `data-on-load fires when the page loads`() {
        val home = """
            <html><body data-on:load="@get('/init')">
                <div id='greeting'>placeholder</div>
            </body></html>
        """.trimIndent()

        val app = routes(
            "/" bind Method.GET to { Response(OK).body(home) },
            "/init" bind Method.GET to {
                Response(OK).body(sseBody(patch("<div id='greeting'>loaded</div>", "#greeting")))
            }
        )

        val driver = driverFor(app)
        driver.get("/")

        assertThat(driver.pageSource, containsSubstring("loaded"))
    }

    @Test
    fun `chained data-on-load introduced by a patch is fired`() {
        val home = """
            <html><body>
                <div id='slot' data-on:load="@get('/step1')"></div>
            </body></html>
        """.trimIndent()

        val app = routes(
            "/" bind Method.GET to { Response(OK).body(home) },
            "/step1" bind Method.GET to {
                Response(OK).body(sseBody(patch("""<div id='slot' data-on:load="@get('/step2')">step1</div>""", "#slot")))
            },
            "/step2" bind Method.GET to {
                Response(OK).body(sseBody(patch("<div id='slot'>step2</div>", "#slot")))
            }
        )

        val driver = driverFor(app)
        driver.get("/")

        assertThat(driver.pageSource, containsSubstring("step2"))
    }

    @Test
    fun `multiple events in one response all apply`() {
        val home = """
            <html><body>
                <button id='btn' data-on:click="@get('/multi')">go</button>
                <div id='a'>old-a</div>
                <div id='b'>old-b</div>
            </body></html>
        """.trimIndent()

        val app = routes(
            "/" bind Method.GET to { Response(OK).body(home) },
            "/multi" bind Method.GET to {
                Response(OK).body(
                    sseBody(
                        patch("<div id='a'>new-a</div>", "#a"),
                        patch("<div id='b'>new-b</div>", "#b")
                    )
                )
            }
        )

        val driver = driverFor(app)
        driver.get("/")

        driver.findElement(By.id("btn")).click()

        assertThat(driver.pageSource, containsSubstring("new-a"))
        assertThat(driver.pageSource, containsSubstring("new-b"))
    }

    @Test
    fun `action requests carry the datastar-request header`() {
        var seenHeader: String? = null
        val home = """
            <html><body>
                <button id='btn' data-on:click="@get('/probe')">go</button>
            </body></html>
        """.trimIndent()

        val app = routes(
            "/" bind Method.GET to { Response(OK).body(home) },
            "/probe" bind Method.GET to { req ->
                seenHeader = req.header("datastar-request")
                Response(OK).body(sseBody(patch("<div id='out'>x</div>", "#out")))
            }
        )

        val driver = driverFor(app)
        driver.get("/")
        driver.findElement(By.id("btn")).click()

        assertThat(seenHeader, equalTo("true"))
    }
}
