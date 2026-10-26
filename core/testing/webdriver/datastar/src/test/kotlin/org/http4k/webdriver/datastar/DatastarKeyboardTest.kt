package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.openqa.selenium.By
import org.openqa.selenium.Keys

class DatastarKeyboardTest {

    private fun pageWith(body: String) = driverFor(
        probeApp(
            $$"""<html><body data-signals="{count: 0, keys: ''}">
                <span id='out' data-text="$count"></span>
                $$body
            </body></html>""",
            mutableListOf()
        )
    ).apply { get("http://localhost/") }

    @Test
    fun `keydown fires once per key with the key name`() {
        val driver = pageWith($$"""<input id='in' data-on:keydown="$keys = $keys + evt.key + ','">
            <span id='keys' data-text="$keys"></span>""")

        driver.findElement(By.id("in")).sendKeys("ab", Keys.ENTER, Keys.ESCAPE)

        assertThat(driver.findElement(By.id("keys")).text, equalTo("a,b,Enter,Escape,"))
    }

    @Test
    fun `keydown on the window fires whichever element the keys are sent to`() {
        val driver = pageWith($$"""<div data-on:keydown__window="evt.key === 'Escape' && $count++"></div><input id='in'>""")

        driver.findElement(By.id("in")).sendKeys(Keys.ESCAPE)

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
    }

    @Test
    fun `window is only modelled for keydown`() {
        assertThrows<UnsupportedDatastarExpression> { pageWith("<div data-on:click__window='1'></div>") }
    }
}
