package org.http4k.storyboard.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarComputedIndicatorTest {

    private val probes = mutableListOf<Request>()

    private fun appWith(home: String, vararg extraRoutes: RoutingHttpHandler): HttpHandler = routes(
        "/" bind Method.GET to { Response(OK).body(home) },
        "/probe" bind Method.GET to { probes.add(it); Response(OK).body("") },
        *extraRoutes
    )

    private fun driverFor(app: HttpHandler) = DatastarWebDriver(Http4kWebDriver(app), app)

    @Test
    fun `data-computed derives signals and reacts to changes`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{price: 10, qty: 2}" data-computed-total="${'$'}price * ${'$'}qty">
                <span id='out' data-text="${'$'}total"></span>
                <button id='more' data-on-click="${'$'}qty++">+</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        assertThat(driver.findElement(By.id("out")).text, equalTo("20"))

        driver.findElement(By.id("more")).click()
        assertThat(driver.findElement(By.id("out")).text, equalTo("30"))
    }

    @Test
    fun `computed signals may depend on other computed signals`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{net: 100}"
                data-computed-gross="${'$'}net * 1.2" data-computed-display="'gross: ' + ${'$'}gross">
                <span id='out' data-text="${'$'}display"></span>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")

        assertThat(driver.findElement(By.id("out")).text, equalTo("gross: 120"))
    }

    @Test
    fun `computed signals are sent to the backend`() {
        val driver = driverFor(
            appWith(
                """<html><body data-signals="{price: 10, qty: 3}" data-computed-total="${'$'}price * ${'$'}qty">
                <button id='send' data-on-click="@get('/probe')">send</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("send")).click()

        assertThat(probes.last().query("datastar")!!, containsSubstring(""""total":30"""))
    }

    @Test
    fun `data-indicator is true during the request and false afterwards`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <button id='go' data-on-click="@get('/probe')" data-indicator-fetching>go</button>
                <div id='spinner' data-show="${'$'}fetching">loading...</div>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        assertThat(driver.findElement(By.id("spinner")).isDisplayed, equalTo(false))

        driver.findElement(By.id("go")).click()

        assertThat(probes.last().query("datastar"), equalTo("""{"fetching":true}"""))
        assertThat(driver.findElement(By.id("spinner")).isDisplayed, equalTo(false))
    }

    @Test
    fun `data-indicator value form names the signal`() {
        val driver = driverFor(
            appWith(
                """<html><body>
                <button id='go' data-on-click="@get('/probe')" data-indicator="in-flight.saving">go</button>
            </body></html>"""
            )
        )
        driver.get("http://localhost/")
        driver.findElement(By.id("go")).click()

        assertThat(probes.last().query("datastar"), equalTo("""{"in-flight":{"saving":true}}"""))
    }
}
