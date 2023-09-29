package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.path
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.File

class Http4kHtmxWebDriverTest {
    val body = File("src/test/resources/test_htmx.html").readText()
    private val driver = Http4kHtmxWebDriver { req ->
        when {
            req.uri.path == "/text" -> Response(Status.OK).body(req.query("value") ?: "")
            req.uri.path == "/div" -> Response(Status.OK).body("""<div id="${req.query("id")}" ${req.query("htmx-action")}="${req.query("htmx-target")}">${req.query("value")}</div>""")
            else -> Response(Status.OK).body(body)
        }
    }

    @Test
    fun `test the driver`() {
        driver.navigate().to("/text?value=foo")
        assertThat(driver.pageSource, equalTo("foo"))

        driver.navigate().to("/div?id=1&htmx-action=hx-get&htmx-target=%2Ftext%3Fvalue%3Dbar&value=foo")
        assertThat(driver.pageSource, equalTo("""<div id="1" hx-get="/text?value=bar">foo</div>"""))

        driver.navigate().to("/")
        assertThat(driver.pageSource!!, containsSubstring("<html"))
    }

    @Test
    fun `issues a GET request on click and swaps text content`() {
        driver.navigate().to("/")

        val button = driver.findElement(By.id("hx-get-button"))!!
        button.click()

        assertThat(button.text, equalTo("foo"))
    }
    @Test
    fun `issues a GET request on click and swaps html content`() {
        driver.navigate().to("/")

        val div = driver.findElement(By.id("hx-get-div"))!!
        div.click()

        val loadedDiv = driver.findElement(By.id("loaded-div"))!!
        loadedDiv.click()

        assertThat(loadedDiv.text, equalTo("bar"))
    }


}
