/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.storyboard.frame.StoryboardWebDriver
import org.http4k.storyboard.util.gzipBase64Decode
import org.http4k.storyboard.util.gzipBase64DecodeBytes
import org.http4k.webdriver.Http4kWebDriver
import org.junit.jupiter.api.Test

class StoryboardWebDriverTest {

    private val homeHtml = "<html><head><title>Home</title></head><body><h1>hi</h1></body></html>"
    private val handler: HttpHandler = { Response(OK).body(homeHtml) }

    @Test
    fun `capture records current page source as gzip base64`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/home")
            it.capture("Home page", "first load")
        }

        assertThat(frames, hasSize(equalTo(1)))
        val only = frames.single()
        assertThat(only.title, equalTo("Home page"))
        assertThat(only.notes, equalTo("first load"))
        assertThat(only.dom.gzipBase64Decode(), equalTo(homeHtml))
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

    @Test
    fun `capture stores local assets referenced by the page in domAssets`() {
        val html = """<html><head><link rel="stylesheet" href="/style.css"></head><body><img src="/logo.png"></body></html>"""
        val cssBody = "body { color: rebeccapurple; }"
        val pngBytes = ByteArray(4) { it.toByte() }
        val app: HttpHandler = { req ->
            when (req.uri.path) {
                "/home" -> Response(OK).body(html)
                "/style.css" -> Response(OK).body(cssBody)
                "/logo.png" -> Response(OK).body(MemoryBody(pngBytes))
                else -> Response(NOT_FOUND)
            }
        }

        val frames = recordFrames(app) {
            it.get("http://localhost/home")
            it.capture("Home")
        }

        val only = frames.single()
        assertThat(only.domAssets.keys, equalTo(setOf("/style.css", "/logo.png")))
        assertThat(only.domAssets["/style.css"]!!.gzipBase64Decode(), equalTo(cssBody))
        assertThat(only.domAssets["/logo.png"]!!.gzipBase64DecodeBytes().toList(), equalTo(pngBytes.toList()))
    }

    @Test
    fun `capture leaves domAssets empty when page has no local refs`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/home")
            it.capture("Home")
        }
        assertThat(frames.single().domAssets, equalTo(emptyMap()))
    }
}
