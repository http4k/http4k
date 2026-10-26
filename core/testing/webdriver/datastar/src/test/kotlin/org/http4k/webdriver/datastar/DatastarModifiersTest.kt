package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarModifiersTest {

    private fun pageWith(button: String) = driverFor(
        probeApp(
            $$"""<html><body data-signals="{count: 0}">
                <span id='out' data-text="$count"></span>
                $$button
            </body></html>""",
            mutableListOf()
        )
    ).apply { get("http://localhost/") }

    @Test
    fun `timing and listener modifiers are ignored so the handler fires straight away`() {
        val driver = pageWith(
            $$"""<button id='inc' data-on:click__debounce.500ms__throttle.1s__delay.1s__viewtransition__passive__capture__prevent__stop__case.kebab="$count++">+</button>"""
        )

        driver.findElement(By.id("inc")).click()

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
    }

    @Test
    fun `once fires the handler only the first time`() {
        val driver = pageWith($$"""<button id='inc' data-on:click__once="$count++">+</button>""")

        driver.findElement(By.id("inc")).click()
        driver.findElement(By.id("inc")).click()

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
    }

    @Test
    fun `a submit handler with a modifier fires on submit`() {
        val driver = pageWith($$"""<form id='f' data-on:submit__prevent="$count++"><input id='in' name='x'></form>""")

        driver.findElement(By.id("in")).submit()

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
    }
}
