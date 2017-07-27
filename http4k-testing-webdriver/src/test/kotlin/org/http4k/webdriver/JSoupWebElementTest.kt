package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jsoup.Jsoup
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.OutputType

class JSoupWebElementTest {

    val element = JSoupWebElement(Jsoup.parse("""<a id="bob"><span>hello</span></a>""")).findElement(By.tagName("a"))!!

    @Test
    fun `find sub element`() = assertThat(element.findElement(By.tagName("span"))!!.text, equalTo("hello"))

    @Test
    fun `find sub elements`() = assertThat(element.findElements(By.tagName("span"))[0].text, equalTo("hello"))

    @Test
    fun `tag name`() = assertThat(element.tagName, equalTo("a"))

    @Test
    fun `attribute`() = assertThat(element.getAttribute("id"), equalTo("bob"))

    @Test
    fun `text`() = assertThat(element.text, equalTo("hello"))

    @Test
    fun `unsupported features`() {
        isNotImplemented {element.isDisplayed}
        isNotImplemented {element.isEnabled}
        isNotImplemented {element.isSelected}
        isNotImplemented {element.clear()}
        isNotImplemented {element.click()}
        isNotImplemented {element.submit()}
        isNotImplemented {element.sendKeys("")}
        isNotImplemented {element.location}
        isNotImplemented {element.rect}
        isNotImplemented {element.size}
        isNotImplemented {element.getScreenshotAs(OutputType.FILE)}
        isNotImplemented {element.getCssValue("some value")}
    }
}