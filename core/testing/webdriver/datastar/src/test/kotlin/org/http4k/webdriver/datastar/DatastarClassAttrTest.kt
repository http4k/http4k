package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarClassAttrTest {

    private fun driverFor(home: String): DatastarWebDriver =
        driverFor(routes("/" bind Method.GET to { Response(OK).body(home) }) as HttpHandler)

    @Test
    fun `data-class-star toggles a class and preserves static ones`() {
        val driver = driverFor(
            $$"""<html><body data-signals="{active: false}">
            <div id='box' class='static' data-class:highlighted="$active">box</div>
            <button id='toggle' data-on:click="$active = !$active">toggle</button>
        </body></html>"""
        )
        driver.get("/")
        assertThat(driver.findElement(By.id("box")).getDomAttribute("class"), equalTo("static"))

        driver.findElement(By.id("toggle")).click()
        assertThat(driver.findElement(By.id("box")).getDomAttribute("class"), equalTo("static highlighted"))

        driver.findElement(By.id("toggle")).click()
        assertThat(driver.findElement(By.id("box")).getDomAttribute("class"), equalTo("static"))
    }

    @Test
    fun `data-class object syntax toggles multiple classes`() {
        val driver = driverFor(
            $$"""<html><body data-signals="{count: 5}">
            <div id='box' data-class="{'text-bold': $count > 3, 'text-dim': $count <= 3}">box</div>
        </body></html>"""
        )
        driver.get("/")

        assertThat(driver.findElement(By.id("box")).getDomAttribute("class"), equalTo("text-bold"))
    }

    @Test
    fun `data-attr-star sets and removes attributes`() {
        val driver = driverFor(
            $$"""<html><body data-signals="{count: 0}">
            <button id='save' data-attr:disabled="$count == 0">save</button>
            <button id='inc' data-on:click="$count++">+</button>
        </body></html>"""
        )
        driver.get("/")
        assertThat(driver.findElement(By.id("save")).isEnabled, equalTo(false))

        driver.findElement(By.id("inc")).click()
        assertThat(driver.findElement(By.id("save")).isEnabled, equalTo(true))
    }

    @Test
    fun `data-attr-star sets string values`() {
        val driver = driverFor(
            $$"""<html><body data-signals="{name: 'bob'}">
            <div id='box' data-attr:title="'hello ' + $name">box</div>
        </body></html>"""
        )
        driver.get("/")

        assertThat(driver.findElement(By.id("box")).getDomAttribute("title"), equalTo("hello bob"))
    }

    @Test
    fun `data-attr object syntax sets multiple attributes`() {
        val driver = driverFor(
            $$"""<html><body data-signals="{expanded: true}">
            <div id='box' data-attr="{'aria-expanded': $expanded ? 'true' : 'false', role: 'region'}">box</div>
        </body></html>"""
        )
        driver.get("/")

        assertThat(driver.findElement(By.id("box")).getDomAttribute("aria-expanded"), equalTo("true"))
        assertThat(driver.findElement(By.id("box")).getDomAttribute("role"), equalTo("region"))
    }
}
