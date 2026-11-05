package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.webdriver.DragEvent.Drop
import org.http4k.webdriver.DragEvent.Enter
import org.http4k.webdriver.DragEvent.Leave
import org.http4k.webdriver.DragEvent.Over
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.openqa.selenium.By
import org.openqa.selenium.ElementNotInteractableException

class DatastarDragTest {

    private fun pageWith(body: String) = driverFor(
        probeApp(
            $$"""<html><body data-signals="{count: 0}" data-on:dragenter__window="$_dropping = true">
                <span id='out' data-text="$count"></span>
                $$body
            </body></html>""",
            mutableListOf()
        )
    ).apply { get("http://localhost/") }

    private val overlay = $$"""<div id='overlay' style='display: none' data-show="$_dropping">
        <input id='file' data-on:dragleave="$_dropping = false" data-on:drop="$_dropping = false; $count++">
    </div><p id='text'></p>"""

    @Test
    fun `a drag entering anything reaches the window`() {
        val driver = pageWith(overlay)

        driver.drag(Enter, By.id("text"))

        assertThat(driver.findElement(By.id("overlay")).isDisplayed, equalTo(true))
    }

    @Test
    fun `a drag bubbles up through the target's parents`() {
        val driver = pageWith($$"""<div data-on:dragover="$count++"><p id='inner'></p></div>""")

        driver.drag(Over, By.id("inner"))

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
    }

    @Test
    fun `leaving takes a drag away`() {
        val driver = pageWith(overlay)
        driver.drag(Enter, By.id("text"))

        driver.drag(Leave, By.id("file"))

        assertThat(driver.findElement(By.id("overlay")).isDisplayed, equalTo(false))
    }

    @Test
    fun `dropping fires on the target`() {
        val driver = pageWith(overlay)
        driver.drag(Enter, By.id("text"))

        driver.drag(Drop, By.id("file"))

        assertThat(driver.findElement(By.id("out")).text, equalTo("1"))
        assertThat(driver.findElement(By.id("overlay")).isDisplayed, equalTo(false))
    }

    @Test
    fun `nothing can be dropped on what isn't showing`() {
        val driver = pageWith(overlay)

        assertThrows<ElementNotInteractableException> { driver.drag(Drop, By.id("file")) }
    }
}
