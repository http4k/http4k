package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.io.File

class JSoupElementFinderTest {
    private val contents = File("src/test/resources/test.html").readText()

    private val state = JSoupElementFinder({}, { null }, Jsoup.parse(contents))

    @Test
    fun `find element`() = assertThat(state.findElement(By.tagName("span"))!!.text, equalTo("this is a span"))

    @Test
    fun `find elements`() = assertThat(state.findElements(By.tagName("span"))[0].text, equalTo("this is a span"))
}
