package org.http4k.storyboard.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.datastar.Signal
import org.http4k.routing.bind
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarWebDriverSignalsTest {

    private val probes = mutableListOf<Request>()

    private fun appWith(home: String, vararg extraRoutes: org.http4k.routing.RoutingHttpHandler): HttpHandler =
        probeApp(home, probes, *extraRoutes)

    private fun lastSignalsSentViaGet() = probes.last().query("datastar")

    @Test
    fun `data-signals initialises the store and signals are sent with GET actions`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 1, user: {name: 'bob'}}">
                <button id='btn' data-on-click="@get('/probe')">go</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("btn")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":1,"user":{"name":"bob"}}"""))
    }

    @Test
    fun `data-signals-star initialises a single signal with kebab converted to camel`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals-my-count="41">
                <button id='btn' data-on-click="${'$'}myCount++; @get('/probe')">go</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("btn")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"myCount":42}"""))
    }

    @Test
    fun `non-GET actions send signals as a JSON body`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 7}">
                <button id='btn' data-on-click="@post('/probe')">go</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("btn")).click()

        assertThat(probes.last().bodyString(), equalTo("""{"count":7}"""))
        assertThat(probes.last().header("Content-Type"), equalTo("application/json"))
    }

    @Test
    fun `local underscore signals are not sent to the backend`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 1, _secret: 'shh'}">
                <button id='btn' data-on-click="@get('/probe')">go</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("btn")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":1}"""))
    }

    @Test
    fun `click expressions can update signals without a backend action`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 0}">
                <button id='inc' data-on-click="${'$'}count++">+</button>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        repeat(3) { driver.findElement(By.id("inc")).click() }
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":3}"""))
    }

    @Test
    fun `patch-signals events from the backend merge into the store`() {
        val app = appWith(
            """<html><body data-signals="{count: 1, user: {name: 'bob'}}">
                <button id='load' data-on-click="@get('/update')">load</button>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>""",
            "/update" bind Method.GET to {
                Response(OK).body(sseBody(PatchSignals(Signal.of("""{"count": 2, "user": {"age": 42}}""")).toSseEvent()))
            }
        )
        val driver = driverFor(app)
        driver.get("http://localhost/")
        driver.findElement(By.id("load")).click()
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":2,"user":{"name":"bob","age":42}}"""))
    }

    @Test
    fun `patch-signals with onlyIfMissing does not overwrite`() {
        val app = appWith(
            """<html><body data-signals="{count: 1}">
                <button id='load' data-on-click="@get('/update')">load</button>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>""",
            "/update" bind Method.GET to {
                Response(OK).body(
                    sseBody(PatchSignals(Signal.of("""{"count": 99, "fresh": true}"""), onlyIfMissing = true).toSseEvent())
                )
            }
        )
        val driver = driverFor(app)
        driver.get("http://localhost/")
        driver.findElement(By.id("load")).click()
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":1,"fresh":true}"""))
    }

    @Test
    fun `data-signals with ifmissing modifier does not overwrite existing signals`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 1}">
                <div data-signals__ifmissing="{count: 99, fresh: true}"></div>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":1,"fresh":true}"""))
    }

    @Test
    fun `signals on elements introduced by patches are initialised`() {
        val app = appWith(
            """<html><body data-signals="{count: 1}">
                <div id='slot'></div>
                <button id='load' data-on-click="@get('/update')">load</button>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>""",
            "/update" bind Method.GET to {
                Response(OK).body(
                    sseBody(
                        org.http4k.datastar.DatastarEvent.PatchElements(
                            """<div id='slot' data-signals="{extra: 'yes'}">patched</div>""",
                            selector = org.http4k.datastar.Selector.of("#slot")
                        ).toSseEvent()
                    )
                )
            }
        )
        val driver = driverFor(app)
        driver.get("http://localhost/")
        driver.findElement(By.id("load")).click()
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":1,"extra":"yes"}"""))
    }

    @Test
    fun `full page navigation resets the store`() {
        val app = appWith(
            """<html><body data-signals="{count: 1}">
                <a id='away' href='/two'>away</a>
            </body></html>""",
            "/two" bind Method.GET to {
                Response(OK).body(
                    """<html><body data-signals="{other: true}">
                    <button id='send' data-on-click="@get('/probe')">send</button>
                </body></html>"""
                )
            }
        )
        val driver = driverFor(app)
        driver.get("http://localhost/")
        driver.findElement(By.id("away")).click()
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"other":true}"""))
    }

    @Test
    fun `data-on-load can update signals`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 1}" data-on-load="${'$'}count = 10">
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSentViaGet(), equalTo("""{"count":10}"""))
    }
}
