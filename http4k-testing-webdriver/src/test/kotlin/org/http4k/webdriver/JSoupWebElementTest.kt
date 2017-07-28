package org.http4k.webdriver

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.jsoup.Jsoup
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.OutputType

class JSoupWebElementTest {

    private var newLocation: Pair<Method, String>? = null
    private fun element(tag: String = "a") = JSoupWebElement({ method, url -> newLocation = method to url }, Jsoup.parse("""<$tag id="bob" href="/link">
        |<span>hello</span>
        |<disabled disabled>disabled</disabled>
        |</$tag>""".trimMargin())).findElement(By.tagName(tag))!!

    private fun form() = JSoupWebElement({ method, url -> newLocation = method to url }, Jsoup.parse("""
        <form method="POST" action="/posted">
            <p>inner</p>
        </form>
        """)).findElement(By.tagName("form"))!!

    @Test
    fun `find sub elements`() = assertThat(element().findElements(By.tagName("span"))[0].text, equalTo("hello"))

    @Test
    fun `tag name`() = assertThat(element().tagName, equalTo("a"))

    @Test
    fun `attribute`() = assertThat(element().getAttribute("id"), equalTo("bob"))

    @Test
    fun `text`() = assertThat(element().text, equalTo("hello disabled"))

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
        form().findElement(By.tagName("p"))!!.submit()
        assertThat(newLocation, equalTo(POST to "/posted"))
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
    fun `unsupported features`() {
        isNotImplemented { element().isDisplayed }
        isNotImplemented { element().isSelected }
        isNotImplemented { element().clear() }
        isNotImplemented { element().sendKeys("") }
        isNotImplemented { element().location }
        isNotImplemented { element().rect }
        isNotImplemented { element().size }
        isNotImplemented { element().getScreenshotAs(OutputType.FILE) }
        isNotImplemented { element().getCssValue("some value") }
    }
}