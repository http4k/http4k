package org.http4k.webdriver

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.jsoup.Jsoup
import org.junit.Ignore
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement

class JSoupWebElementTest {

    private var newLocation: Pair<Method, String>? = null
    private val navigate: (Method, String) -> Unit = { method, url -> newLocation = method to url }

    private fun input(type: String): WebElement = JSoupWebElement(navigate, Jsoup.parse("""<input id="bob" value="someValue" type="$type">""")).findElement(By.tagName("input"))!!

    private fun select(multiple: Boolean): WebElement =
        JSoupWebElement(navigate, Jsoup.parse("""<select name="bob" ${if(multiple) "multiple" else ""}>
            <option>foo1</option>
            <option>foo2</option>
            </select>"""
        )).findElement(By.tagName("select"))!!

    private fun element(tag: String = "a"): WebElement =
        JSoupWebElement(navigate, Jsoup.parse("""<$tag id="bob" href="/link">
        |<span>hello</span>
        |<disabled disabled>disabled</disabled>
        |</$tag>""".trimMargin())).findElement(By.tagName(tag))!!

    private fun form(method: Method = POST) = JSoupWebElement({ actual, url -> newLocation = actual to url }, Jsoup.parse("""
        <form method="${method.name}" action="/posted">
            <input id="text" type="text"/>
            <textarea id="textarea"/>
            <p>inner</p>
        </form>
        """)).findElement(By.tagName("form"))!!

    @Test
    fun `find sub elements`() = assertThat(element().findElements(By.tagName("span"))[0].text, equalTo("hello"))

    @Test
    fun `equality is based on jsoup element`() = assertThat(element(), equalTo(element()))

    @Test
    fun `tag name`() = assertThat(element().tagName, equalTo("a"))

    @Test
    fun `attribute`() = assertThat(element().getAttribute("id"), equalTo("bob"))

    @Test
    fun `text`() = assertThat(element().text, equalTo("hello disabled"))

    @Test
    fun `click checkbox or radio`() {
        fun assertCheckableSetsValue(type: String) {
            val input = input(type)
            input.click()
            assertThat(input.isSelected, equalTo(true))
        }

        assertCheckableSetsValue("checkbox")
        assertCheckableSetsValue("radio")
    }

    @Test
    fun `click link`() {
        element("a").click()
        assertThat(newLocation, equalTo(GET to "/link"))
    }

    @Test
    fun `click non-link`() {
        element("foo").click()
        assertThat(newLocation, absent())
    }

    @Test
    fun `submit a form`() {
        form().submit()
        assertThat(newLocation, equalTo(POST to "/posted"))
    }

    @Test
    fun `submit an element inside the form`() {
        form(DELETE).findElement(By.tagName("p"))!!.submit()
        assertThat(newLocation, equalTo(DELETE to "/posted"))
    }

    @Test
    fun `submit a non-form`() {
        element().submit()
        assertThat(newLocation, absent())
    }

    @Test
    fun `disabled`() {
        assertThat(element().isEnabled, equalTo(true))
        assertThat(element().findElement(By.tagName("disabled")).isEnabled, equalTo(false))
    }

    @Test
    fun `send keys to an input`() {
        fun assertKeysSetValue(type: String) {
            val input = input(type)
            input.sendKeys("hello")
            assertThat(input.getAttribute("value"), equalTo("hello"))
        }

        assertKeysSetValue("text")
        assertKeysSetValue("date")
        assertKeysSetValue("password")
    }

    @Test
    @Ignore
    fun `send keys to an single select`() {
        val select = select(false)
        select.findElements(By.tagName("option")).first().click()
        select.findElements(By.tagName("option")).last().click()
        assertThat(select.findElements(By.tagName("option")).first().isSelected, equalTo(false))
        assertThat(select.findElements(By.tagName("option")).last().isSelected, equalTo(true))
    }

    @Test
    fun `send keys to a multi select`() {
        val select = select(true)
        select.findElements(By.tagName("option")).first().click()
        select.findElements(By.tagName("option")).last().click()
        assertThat(select.findElements(By.tagName("option")).first().isSelected, equalTo(true))
        assertThat(select.findElements(By.tagName("option")).last().isSelected, equalTo(true))
    }

    @Test
    fun `send keys to an textArea`() {
        val input = form().findElement(By.id("textarea"))
        input.sendKeys("hello")
        assertThat(input.text, equalTo("hello"))
    }

    @Test
    fun `unsupported features`() {
        isNotImplemented { element().isDisplayed }
        isNotImplemented { element().clear() }
        isNotImplemented { element().location }
        isNotImplemented { element().rect }
        isNotImplemented { element().size }
        isNotImplemented { element().getScreenshotAs(OutputType.FILE) }
        isNotImplemented { element().getCssValue("some value") }
    }
}