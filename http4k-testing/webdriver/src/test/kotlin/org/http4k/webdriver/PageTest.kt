package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class PageTest {
    private val contents = File("src/test/resources/test.html").readText()

    private val state = Page(OK, {}, { null }, UUID.randomUUID(), "someUrl", contents)

    @Test
    fun title() = assertThat(state.title, equalTo("Page title"))

    @Test
    fun `find elements`() = assertThat(state.findElement(By.tagName("span"))!!.text, equalTo("this is a span"))

    @Test
    fun `first element`() = assertThat(state.firstElement()!!.tagName, equalTo("div"))
}
