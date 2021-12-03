package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.io.File

class ByTest {
    private val contents = File("src/test/resources/test.html").readText()

    private val state = JSoupElementFinder({}, { null }, Jsoup.parse(contents))

    @Test
    fun `find by class`() = assertThat(By.className("aClass").findElement(state).text, equalTo("the first text"))

    @Test
    fun `find by id`() = assertThat(By.id("firstId").findElement(state).text, equalTo("the first text"))

    @Test
    fun `find by css`() = assertThat(By.cssSelector(".aClass").findElement(state).text, equalTo("the first text"))

    @Test
    fun `find by disabled css`() = assertThat(By.disabledCssSelector("disabled").findElement(state).text, equalTo("this is a disabled item"))

    @Test
    fun `find by id using wrong API throws`() = assertThat({ org.openqa.selenium.By.id("firstId").findElement(state) }, throws<ClassCastException>())
}
