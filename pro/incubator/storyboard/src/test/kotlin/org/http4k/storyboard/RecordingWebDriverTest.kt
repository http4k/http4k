package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.Test
import java.util.Base64

class RecordingWebDriverTest {

    private val homeHtml = "<html><head><title>Home</title></head><body><h1>hi</h1></body></html>"
    private val handler: HttpHandler = { Response(OK).body(homeHtml) }
    private val driver = RecordingWebDriver(Http4kWebDriver(handler))

    @Test
    fun `capture records current page source as base64`() {
        driver.get("http://localhost/home")

        driver.capture("Home page", "first load")

        val frames = driver.frames()
        assertThat(frames, hasSize(equalTo(1)))
        val only = frames.single()
        assertThat(only.title, equalTo("Home page"))
        assertThat(only.notes, equalTo("first load"))
        assertThat(decodeBase64(only.dom), equalTo(homeHtml))
    }

    @Test
    fun `capture defaults notes to empty string`() {
        driver.get("http://localhost/home")

        driver.capture("Just a title")

        assertThat(driver.frames().single().notes, equalTo(""))
    }

    @Test
    fun `frames accumulate in order`() {
        driver.get("http://localhost/home")

        driver.capture("first")
        driver.capture("second")
        driver.capture("third")

        assertThat(driver.frames().map { it.title }, equalTo(listOf("first", "second", "third")))
    }

    @Test
    fun `frames returns an immutable copy`() {
        driver.get("http://localhost/home")
        driver.capture("one")

        val copy = driver.frames()
        driver.capture("two")

        assertThat(copy.map { it.title }, equalTo(listOf("one")))
        assertThat(driver.frames().map { it.title }, equalTo(listOf("one", "two")))
    }

    @Test
    fun `delegates page source to underlying driver`() {
        driver.get("http://localhost/home")

        assertThat(driver.pageSource, equalTo(homeHtml))
    }

    @Test
    fun `delegates title to underlying driver`() {
        driver.get("http://localhost/home")

        assertThat(driver.title, equalTo("Home"))
    }

    private fun decodeBase64(s: String) = String(Base64.getDecoder().decode(s))
}
