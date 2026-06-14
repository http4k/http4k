/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.storyboard.CaptureMode.ManualOnly
import org.http4k.storyboard.StoryFrame.Kind.Auto
import org.http4k.storyboard.StoryFrame.Kind.Manual
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.util.Base64

class RecordingWebElementTest {

    private val homeHtml = """
        <html><body>
            <a id="link" href="/next">go</a><h1>home</h1>
            <form id="form" action="/next" method="get"><input id="name" type="text" value="seed"/></form>
        </body></html>
    """.trimIndent()
    private val nextHtml = """<html><body><h1>next page</h1></body></html>"""

    private val handler: HttpHandler = { req ->
        when (req.uri.path) {
            "/next" -> Response(OK).body(nextHtml)
            else -> Response(OK).body(homeHtml)
        }
    }

    private val driver = RecordingWebDriver(Http4kWebDriver(handler))

    @Test
    fun `click on element automatically captures a frame`() {
        driver.get("http://localhost/")

        driver.findElement(By.id("link")).click()

        assertThat(driver.frames(), hasSize(equalTo(1)))
    }

    @Test
    fun `auto-capture title contains locator description`() {
        driver.get("http://localhost/")

        driver.findElement(By.id("link")).click()

        val title = driver.frames().single().title
        assertThat(title, containsSubstring("click"))
        assertThat(title, containsSubstring("link"))
    }

    @Test
    fun `auto-capture records DOM after the click navigated`() {
        driver.get("http://localhost/")

        driver.findElement(By.id("link")).click()

        val decoded = String(Base64.getDecoder().decode(driver.frames().single().dom))
        assertThat(decoded, containsSubstring("next page"))
    }

    @Test
    fun `multiple clicks accumulate frames`() {
        driver.get("http://localhost/")

        driver.findElement(By.tagName("h1")).click()
        driver.findElement(By.id("link")).click()

        assertThat(driver.frames(), hasSize(equalTo(2)))
    }

    @Test
    fun `findElement on a wrapped element wraps recursively`() {
        val html = """<html><body><div id="container"><a id="inner" href="/next">deep</a></div></body></html>"""
        val nestedHandler: HttpHandler = { Response(OK).body(html) }
        val nested = RecordingWebDriver(Http4kWebDriver(nestedHandler))

        nested.get("http://localhost/")
        val container = nested.findElement(By.id("container"))
        container.findElement(By.id("inner")).click()

        assertThat(nested.frames(), hasSize(equalTo(1)))
        assertThat(nested.frames().single().title, containsSubstring("inner"))
    }

    @Test
    fun `manual captures interleave with auto-captures`() {
        driver.get("http://localhost/")

        driver.capture("Before")
        driver.findElement(By.id("link")).click()
        driver.capture("After")

        assertThat(driver.frames().map { it.title }.first(), equalTo("Before"))
        assertThat(driver.frames().map { it.title }.last(), equalTo("After"))
        assertThat(driver.frames(), hasSize(equalTo(3)))
    }

    @Test
    fun `auto-captured frames are tagged Auto and manual captures Manual`() {
        driver.get("http://localhost/")

        driver.capture("Before")
        driver.findElement(By.id("link")).click()

        assertThat(driver.frames().map { it.kind }, equalTo(listOf(Manual, Auto)))
    }

    @Test
    fun `manualFrames filters out auto-captures`() {
        driver.get("http://localhost/")

        driver.capture("Before")
        driver.findElement(By.id("link")).click()
        driver.capture("After")

        assertThat(driver.manualFrames().map { it.title }, equalTo(listOf("Before", "After")))
    }

    @Test
    fun `submit auto-captures with the form locator`() {
        driver.get("http://localhost/")

        driver.findElement(By.id("form")).submit()

        val frame = driver.frames().single()
        assertThat(frame.title, equalTo("submit [By.id: form]"))
        assertThat(frame.kind, equalTo(Auto))
    }

    @Test
    fun `clear auto-captures with the input locator`() {
        driver.get("http://localhost/")

        driver.findElement(By.id("name")).clear()

        val frame = driver.frames().single()
        assertThat(frame.title, equalTo("clear [By.id: name]"))
        assertThat(frame.kind, equalTo(Auto))
    }

    @Test
    fun `sendKeys auto-captures the typed string`() {
        driver.get("http://localhost/")

        driver.findElement(By.id("name")).sendKeys("hi")

        val frame = driver.frames().single()
        assertThat(frame.title, equalTo("sendKeys 'hi' [By.id: name]"))
        assertThat(frame.kind, equalTo(Auto))
    }

    @Test
    fun `ManualOnly mode skips auto-captures and keeps manual ones`() {
        val quiet = RecordingWebDriver(Http4kWebDriver(handler), captureMode = ManualOnly)
        quiet.get("http://localhost/")

        quiet.findElement(By.id("link")).click()
        quiet.capture("Only this")

        assertThat(quiet.frames().map { it.title }, equalTo(listOf("Only this")))
    }
}
