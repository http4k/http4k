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
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.frame.StoryboardWebElement
import org.http4k.storyboard.util.gzipBase64Decode
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class StoryboardWebElementTest {

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

    @Test
    fun `click records a frame`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.id("link")).click()
        }

        assertThat(frames, hasSize(equalTo(1)))
    }

    @Test
    fun `click records a frame titled with the locator`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.id("link")).click()
        }

        val title = frames.single().title
        assertThat(title, containsSubstring("click"))
        assertThat(title, containsSubstring("link"))
    }

    @Test
    fun `click records the DOM after navigation`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.id("link")).click()
        }

        val decoded = frames.single().dom.gzipBase64Decode()
        assertThat(decoded, containsSubstring("next page"))
    }

    @Test
    fun `multiple clicks accumulate frames`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.tagName("h1")).click()
            it.findElement(By.id("link")).click()
        }

        assertThat(frames, hasSize(equalTo(2)))
    }

    @Test
    fun `findElement on a wrapped element wraps recursively`() {
        val html = """<html><body><div id="container"><a id="inner" href="/next">deep</a></div></body></html>"""
        val nestedHandler: HttpHandler = { Response(OK).body(html) }

        val frames = recordFrames(nestedHandler) {
            it.get("http://localhost/")
            val container = it.findElement(By.id("container"))
            container.findElement(By.id("inner")).click()
        }

        assertThat(frames, hasSize(equalTo(1)))
        assertThat(frames.single().title, containsSubstring("inner"))
    }

    @Test
    fun `explicit capture and interaction frames interleave in call order`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/")
            it.snapshot("Before")
            it.findElement(By.id("link")).click()
            it.snapshot("After")
        }

        assertThat(frames.map { it.title }.first(), equalTo("Before"))
        assertThat(frames.map { it.title }.last(), equalTo("After"))
        assertThat(frames, hasSize(equalTo(3)))
    }

    @Test
    fun `interactions record at Detail level and capture() at Story level`() {
        val frames = recordFrames(handler) {
            it.get("http://localhost/")
            it.snapshot("Before")
            it.findElement(By.id("link")).click()
        }

        assertThat(frames.map { it.level }, equalTo(listOf(Story, Detail)))
    }

    @Test
    fun `submit records a frame with the form locator`() {
        val frame = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.id("form")).submit()
        }.single()

        assertThat(frame.title, equalTo("submit [By.id: form]"))
        assertThat(frame.level, equalTo(Detail))
    }

    @Test
    fun `clear records a frame with the input locator`() {
        val frame = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.id("name")).clear()
        }.single()

        assertThat(frame.title, equalTo("clear [By.id: name]"))
        assertThat(frame.level, equalTo(Detail))
    }

    @Test
    fun `sendKeys records a frame with the typed string`() {
        val frame = recordFrames(handler) {
            it.get("http://localhost/")
            it.findElement(By.id("name")).sendKeys("hi")
        }.single()

        assertThat(frame.title, equalTo("sendKeys 'hi' [By.id: name]"))
        assertThat(frame.level, equalTo(Detail))
    }

    @Test
    fun `selector exposes the leaf locator for each supported By type`() {
        val html = """
            <html><body>
                <a id="link-id" class="link-class" href="/x">link text</a>
                <h1>heading</h1>
            </body></html>
        """.trimIndent()
        val byTypeHandler: HttpHandler = { Response(OK).body(html) }

        val cases = listOf(
            By.id("link-id"),
            By.className("link-class"),
            By.tagName("h1"),
            By.cssSelector("#link-id"),
            By.linkText("link text")
        )

        recordStory(byTypeHandler) { driver ->
            driver.get("http://localhost/")
            cases.forEach { by ->
                val element = driver.findElement(by) as StoryboardWebElement
                assertThat("for $by", element.selector.toString(), containsSubstring(by.toString()))
                assertThat("for $by", element.selector.findElement(driver).tagName, equalTo(element.tagName))
            }
        }
    }

    @Test
    fun `selector chains across nested findElement calls`() {
        val html = """<html><body><div id="container"><a id="inner" href="/x">deep</a></div></body></html>"""
        val nestedHandler: HttpHandler = { Response(OK).body(html) }

        recordStory(nestedHandler) { driver ->
            driver.get("http://localhost/")
            val container = driver.findElement(By.id("container")) as StoryboardWebElement
            val inner = container.findElement(By.id("inner")) as StoryboardWebElement

            assertThat(inner.selector.toString(), containsSubstring("By.id: container"))
            assertThat(inner.selector.toString(), containsSubstring("By.id: inner"))
            assertThat(inner.selector.findElement(driver).getAttribute("id"), equalTo("inner"))
        }
    }
}
