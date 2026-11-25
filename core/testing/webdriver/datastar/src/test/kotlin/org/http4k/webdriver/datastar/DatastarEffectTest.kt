package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.openqa.selenium.By

class DatastarEffectTest {

    private fun load(body: String) =
        driverFor(probeApp("<html><body>$body</body></html>", mutableListOf())).apply { get("http://localhost/") }

    @Test
    fun `an effect writes the element's text from the signals, live as they change`() {
        val driver = load(
            """<div data-signals="{name: 'ann'}"></div><p id="out" data-effect="el.textContent = 'hi ' + ${'$'}name"></p><button id="b" data-on:click="${'$'}name = 'bob'">b</button>"""
        )

        assertThat(driver.findElement(By.id("out")).text, equalTo("hi ann"))

        driver.findElement(By.id("b")).click()

        assertThat(driver.findElement(By.id("out")).text, equalTo("hi bob"))
    }

    @Test
    fun `an effect focusing the element when a signal turns on loads`() {
        load("""<div data-signals="{editing: true}"></div><input data-effect="${'$'}editing && el.focus()">""")
    }

    @Test
    fun `an effect doing anything else to the element fails the page load`() {
        val failure = assertThrows<UnsupportedDatastarExpression> { load("""<input data-effect="el.remove()">""") }

        assertThat(failure.attribute, equalTo("data-effect"))
    }
}
