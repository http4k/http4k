package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.File

class HtmxHttp4kWebDriverTest {
    val body = File("src/test/resources/test_htmx.html").readText()
    private val driver = Http4kWebDriver { req ->
        when (req.uri.path) {
            "/text" ->
                Response(Status.OK)
                    .body(req.query("value") ?: "")
                    .header("Content-Type", ContentType.TEXT_PLAIN.toHeaderValue())
            "/div" ->
                Response(Status.OK)
                    .body("""<div id="${req.query("id")}" ${req.query("htmx-action")}="${req.query("htmx-target")}">${req.query("value")}</div>""")
                    .header("Content-Type", ContentType.TEXT_HTML.toHeaderValue())
            "/example3" -> {
                val page = req.query("page")?.toInt() ?: 1
                Response(Status.OK)
                    .body("""<tr><td>Agent Smith</td><td>void$page@null.org</td><td>$page</td></tr>
                            |<tr id="example3ReplaceMe">
                            |  <td colspan="3">
                            |    <button id="example3button" data-hx-get="/example3?page=${page+1}"
                            |            data-hx-target="#example3ReplaceMe"
                            |            data-hx-swap="outerHTML">
                            |      Load More Agents...
                            |    </button>
                            |  </td>
                            |</tr>
                        """.trimMargin())
                    .header("Content-Type", ContentType.TEXT_HTML.toHeaderValue())
            }
            else -> Response(Status.OK)
                .body(body)
                .header("Content-Type", ContentType.TEXT_HTML.toHeaderValue())
        }
    }.withHtmx()

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

    @Nested
    inner class `Htmx examples` {
        @Test
        fun `Example 3 - Click to Load`() {
            driver.navigate().to("/")

            val tableBody = driver.findElement(By.id("example3tbody"))!!

            assertThat(tableBody.text, equalTo("Load More Agents..."))

            driver.findElement(By.id("example3button"))!!.click()

            assertThat(tableBody.text, equalTo("Agent Smith void1@null.org 1 Load More Agents..."))

            driver.findElement(By.id("example3button"))!!.click()

            assertThat(tableBody.text, equalTo("Agent Smith void1@null.org 1 Agent Smith void2@null.org 2 Load More Agents..."))
        }
    }
}
