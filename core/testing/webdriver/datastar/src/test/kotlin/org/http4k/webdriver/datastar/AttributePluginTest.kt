package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.core.Request
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class AttributePluginTest {

    private val upper = object : AttributePlugin {
        override val name = "upper"

        override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
            element.text(page.evaluate(attribute).toString().uppercase())
        }
    }

    private fun driver(body: String, plugins: List<DatastarPlugin>, probes: MutableList<Request> = mutableListOf()) =
        DatastarWebDriver(probeApp("<html><body>$body</body></html>", probes), plugins = plugins).apply { get("http://localhost/") }

    @Test
    fun `an app models an attribute the driver doesn't, live as signals change`() {
        val driver = driver(
            """<div data-signals="{name: 'ann'}"></div><p id="out" data-upper="${'$'}name"></p><button id="b" data-on:click="${'$'}name = 'bob'">b</button>""",
            listOf(upper)
        )

        assertThat(driver.findElement(By.id("out")).text, equalTo("ANN"))

        driver.findElement(By.id("b")).click()

        assertThat(driver.findElement(By.id("out")).text, equalTo("BOB"))
    }

    @Test
    fun `a datastar attribute becomes modelled once a plugin for it is registered`() {
        val effect = object : AttributePlugin {
            override val name = "effect"
        }

        driver("""<div data-effect="${'$'}x"></div>""", listOf(effect))
    }

    @Test
    fun `registering a plugin under a built-in's name replaces it`() {
        val quotedText = object : AttributePlugin {
            override val name = "text"

            override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
                element.text("[${page.evaluate(attribute)}]")
            }
        }

        val driver = driver("""<p id="out" data-text="'hi'"></p>""", listOf(quotedText))

        assertThat(driver.findElement(By.id("out")).text, equalTo("[hi]"))
    }

    @Test
    fun `a render hook can't send actions`() {
        val probes = mutableListOf<Request>()
        val sending = object : AttributePlugin {
            override val name = "sending"

            override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
                page.evaluate(attribute)
            }
        }

        driver("""<p data-sending="@post('/probe')"></p>""", listOf(sending), probes)

        assertThat(probes, isEmpty)
    }

    @Test
    fun `a plugin is offered the events on its element, and handling one stops the default`() {
        val seen = mutableListOf<String>()
        val clicks = object : AttributePlugin {
            override val name = "clicks"

            override fun handle(event: DatastarDomEvent, element: Element, attribute: DatastarAttribute, page: DatastarPage): Boolean {
                seen += "${event.type} on ${element.id()}"
                return event.type == "click"
            }
        }

        val driver = driver("""<a id="link" href="/elsewhere" data-clicks="">go</a>""", listOf(clicks))
        driver.findElement(By.id("link")).click()

        assertThat(seen, equalTo(listOf("click on link")))
        assertThat(driver.currentUrl.orEmpty(), equalTo("http://localhost/"))
    }
}
