package org.http4k.storyboard.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.datastar.Signal
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarReactivityTest {

    private val probes = mutableListOf<Request>()

    private fun appWith(home: String, vararg extraRoutes: RoutingHttpHandler): HttpHandler =
        probeApp(home, probes, *extraRoutes)

    private fun lastSignalsSent() = probes.last().query("datastar")

    @Test
    fun `data-text renders signal values and reacts to changes`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{count: 1}">
                <span id='out' data-text="'count is ' + ${'$'}count"></span>
                <button id='inc' data-on-click="${'$'}count++">+</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        assertThat(driver.findElement(By.id("out")).text, equalTo("count is 1"))

        driver.findElement(By.id("inc")).click()
        assertThat(driver.findElement(By.id("out")).text, equalTo("count is 2"))
    }

    @Test
    fun `data-show toggles visibility`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{open: false}">
                <div id='panel' data-show="${'$'}open">secret</div>
                <button id='toggle' data-on-click="${'$'}open = !${'$'}open">toggle</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        assertThat(driver.findElement(By.id("panel")).isDisplayed, equalTo(false))
        assertThat(driver.pageSource, containsSubstring("display:none"))

        driver.findElement(By.id("toggle")).click()
        assertThat(driver.findElement(By.id("panel")).isDisplayed, equalTo(true))

        driver.findElement(By.id("toggle")).click()
        assertThat(driver.findElement(By.id("panel")).isDisplayed, equalTo(false))
    }

    @Test
    fun `data-bind initialises the signal from the element value`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <input id='name' data-bind="name" value='bob'/>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSent(), equalTo("""{"name":"bob"}"""))
    }

    @Test
    fun `typing into a bound input updates the signal and dependent elements`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <input id='name' data-bind="name"/>
                <span id='greeting' data-text="'hello ' + ${'$'}name"></span>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("name")).sendKeys("alice")

        assertThat(driver.findElement(By.id("greeting")).text, equalTo("hello alice"))
    }

    @Test
    fun `an existing signal wins over the element value and is written into the element`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{name: 'fromsignals'}">
                <input id='name' data-bind="name" value='fromelement'/>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")

        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("fromsignals"))
    }

    @Test
    fun `patch-signals updates bound elements`() {
        val app = appWith(
            """<html><body data-signals="{name: 'before'}">
                <input id='name' data-bind="name"/>
                <button id='load' data-on-click="@get('/update')">load</button>
            </body></html>""",
            "/update" bind Method.GET to {
                Response(OK).body(sseBody(PatchSignals(Signal.of("""{"name": "after"}""")).toSseEvent()))
            }
        )
        val driver = driverFor(app)
        driver.get("http://localhost/")
        driver.findElement(By.id("load")).click()

        assertThat(driver.findElement(By.id("name")).getDomAttribute("value"), equalTo("after"))
    }

    @Test
    fun `data-bind-star uses the kebab attribute name as the signal path`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <input id='name' data-bind-user-name value='bob'/>
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("send")).click()

        assertThat(lastSignalsSent(), equalTo("""{"userName":"bob"}"""))
    }

    @Test
    fun `checkbox binding works both ways`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{agreed: false}">
                <input type='checkbox' id='agree' data-bind="agreed"/>
                <div id='thanks' data-show="${'$'}agreed">thanks</div>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        assertThat(driver.findElement(By.id("thanks")).isDisplayed, equalTo(false))

        driver.findElement(By.id("agree")).click()
        assertThat(driver.findElement(By.id("thanks")).isDisplayed, equalTo(true))
        assertThat(driver.findElement(By.id("agree")).isSelected, equalTo(true))
    }

    @Test
    fun `data-on-input fires when typing`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{chars: 0}">
                <input id='name' data-bind="name" data-on-input="${'$'}chars = ${'$'}chars + 1"/>
                <span id='out' data-text="${'$'}chars"></span>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("name")).sendKeys("ab")

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
    }

    @Test
    fun `data-on-submit intercepts form submission`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <form data-on-submit="@get('/probe')">
                    <input id='name' data-bind="name" value='bob'/>
                    <button id='go' type='submit'>go</button>
                </form>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("go")).submit()

        assertThat(lastSignalsSent(), equalTo("""{"name":"bob"}"""))
    }

    @Test
    fun `clearing a bound input resets the signal`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <input id='name' data-bind="name" value='bob'/>
                <span id='out' data-text="'[' + ${'$'}name + ']'"></span>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        assertThat(driver.findElement(By.id("out")).text, equalTo("[bob]"))

        driver.findElement(By.id("name")).clear()
        assertThat(driver.findElement(By.id("out")).text, equalTo("[]"))
    }
}
