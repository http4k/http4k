/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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

class StoryboardWebDriverTest {

    private val homeHtml = "<html><head><title>Home</title></head><body><h1>hi</h1></body></html>"
    private val handler: HttpHandler = { Response(OK).body(homeHtml) }

    @Test
    fun `capture records current page source as base64`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/home")
            it.capture("Home page", "first load")
        }

        assertThat(frames, hasSize(equalTo(1)))
        val only = frames.single()
        assertThat(only.title, equalTo("Home page"))
        assertThat(only.notes, equalTo("first load"))
        assertThat(decodeBase64(only.dom), equalTo(homeHtml))
    }

    @Test
    fun `capture defaults notes to empty string`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/home")
            it.capture("Just a title")
        }

        assertThat(frames.single().notes, equalTo(""))
    }

    @Test
    fun `frames accumulate in order`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/home")
            it.capture("first")
            it.capture("second")
            it.capture("third")
        }

        assertThat(frames.map { it.title }, equalTo(listOf("first", "second", "third")))
    }

    @Test
    fun `delegates page source to underlying driver`() {
        storyboard("test") {
            val driver = StoryboardWebDriver(Http4kWebDriver(handler), this)
            driver.get("/home")

            assertThat(driver.pageSource, equalTo(homeHtml))
        }
    }

    @Test
    fun `delegates title to underlying driver`() {
        storyboard("test") {
            val driver = StoryboardWebDriver(Http4kWebDriver(handler), this)
            driver.get("/home")

            assertThat(driver.title, equalTo("Home"))
        }
    }

    private fun decodeBase64(s: String) = String(Base64.getDecoder().decode(s))
}
