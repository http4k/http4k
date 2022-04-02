package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.File

class ByTest {
    private val contents = File("src/test/resources/test.html").readText()

    private val state = JSoupElementFinder({}, { null }, Jsoup.parse(contents))

    @Test
    fun `find by class`() = assertThat(By.className("aClass").findElement(state).text, equalTo("the first text"))

    @Test
    fun `find by id`() = assertThat(By.id("secondId").findElement(state).text, equalTo("the second text"))

    @Test
    fun `find by tag name`() = assertThat(By.tagName("themethod").findElement(state).text, equalTo("THEMETHOD"))

    @Test
    fun `find by css`() = assertThat(By.cssSelector(".aClass").findElement(state).text, equalTo("the first text"))
}
