/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.recordFrames
import org.http4k.storyboard.util.gzipBase64Decode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Base64

class FrameBuilderTest {

    // 1x1 transparent PNG
    private val tinyPng: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    )

    private fun decoded(frame: StoryFrame): String = frame.dom.gzipBase64Decode()

    @Test
    fun `frame records a single StoryFrame at the given level with title and notes`() {
        val frame = recordFrames {
            frame("Step 1", level = Story) {
                notes = "an explanation"
                html("<p>hello</p>")
            }
        }.single()

        assertThat(frame.title, equalTo("Step 1"))
        assertThat(frame.notes, equalTo("an explanation"))
        assertThat(frame.level, equalTo(Story))
    }

    @Test
    fun `frame defaults to Context level and empty notes`() {
        val frame = recordFrames {
            frame("Step 1") { html("<p>hello</p>") }
        }.single()

        assertThat(frame.notes, equalTo(""))
        assertThat(frame.level, equalTo(Context))
    }

    @Test
    fun `frame wraps each section in a labelled wrapper element`() {
        val html = decoded(recordFrames {
            frame("Mixed") {
                html("<p>raw</p>")
            }
        }.single())

        assertThat(html, containsSubstring("""<section class="frame-section"><p>raw</p></section>"""))
    }

    @Test
    fun `frame preserves the order of sections in the assembled body`() {
        val html = decoded(recordFrames {
            frame("Mixed") {
                html("<p>first</p>")
                html("<p>second</p>")
                html("<p>third</p>")
            }
        }.single())

        val firstIdx = html.indexOf("<p>first</p>")
        val secondIdx = html.indexOf("<p>second</p>")
        val thirdIdx = html.indexOf("<p>third</p>")

        assertThat(firstIdx >= 0 && firstIdx < secondIdx && secondIdx < thirdIdx, equalTo(true))
    }

    @Test
    fun `frame composes text, code and image sections in order`(@TempDir dir: File) {
        val codeFile = File(dir, "Sample.kt").apply { writeText("fun hi() = 1") }
        val imgFile = File(dir, "logo.png").apply { writeBytes(tinyPng) }

        val html = decoded(recordFrames {
            frame("Walkthrough") {
                text("Intro line")
                code(codeFile)
                image(imgFile)
            }
        }.single())

        val textIdx = html.indexOf("Intro line")
        val codeIdx = html.indexOf("fun hi() = 1")
        val imageIdx = html.indexOf("data:image/png;base64,")

        assertThat(textIdx in 0 until codeIdx, equalTo(true))
        assertThat(codeIdx < imageIdx, equalTo(true))
    }

    @Test
    fun `text section html-escapes the input`() {
        val html = decoded(recordFrames {
            frame("Prose") { text("a < b & c > d") }
        }.single())

        assertThat(html, containsSubstring("a &lt; b &amp; c &gt; d"))
    }

    @Test
    fun `frame wraps the assembled body in a Prism and Mermaid enabled HTML document`() {
        val html = decoded(recordFrames {
            frame("One") { html("<p>x</p>") }
        }.single())

        assertThat(html, containsSubstring("<!DOCTYPE html>"))
        assertThat(html, containsSubstring("prism.min.css"))
        assertThat(html, containsSubstring("mermaid"))
    }

    @Test
    fun `frame includes default section spacing CSS`() {
        val html = decoded(recordFrames {
            frame("One") { html("<p>x</p>") }
        }.single())

        assertThat(html, containsSubstring(".frame-section + .frame-section"))
    }

    @Test
    fun `section is an escape hatch that appends a custom body`() {
        val html = decoded(recordFrames {
            frame("Custom") { section(Section("<aside>extra</aside>")) }
        }.single())

        assertThat(html, containsSubstring("""<section class="frame-section"><aside>extra</aside></section>"""))
    }

    @Test
    fun `code section reads the entire file when lines is null`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("line one\nline two\nline three") }

        val html = decoded(recordFrames { frame("All") { code(file) } }.single())

        assertThat(html, containsSubstring("line one\nline two\nline three"))
    }

    @Test
    fun `code section snips the given line range using 1-based inclusive bounds`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("one\ntwo\nthree\nfour\nfive") }

        val html = decoded(recordFrames { frame("Middle") { code(file, lines = 2..4) } }.single())

        assertThat(html, containsSubstring("two\nthree\nfour"))
    }

    @Test
    fun `code section infers language from file extension`(@TempDir dir: File) {
        val file = File(dir, "query.sql").apply { writeText("SELECT 1") }

        val html = decoded(recordFrames { frame("Query") { code(file) } }.single())

        assertThat(html, containsSubstring("class=\"language-sql\""))
    }

    @Test
    fun `code section respects an explicit language override`(@TempDir dir: File) {
        val file = File(dir, "config.txt").apply { writeText("a = 1") }

        val html = decoded(recordFrames { frame("Config") { code(file, language = "toml") } }.single())

        assertThat(html, containsSubstring("class=\"language-toml\""))
    }

    @Test
    fun `code section escapes HTML in the source so script tags survive as text`(@TempDir dir: File) {
        val file = File(dir, "danger.js").apply { writeText("<script>alert('x')</script>") }

        val html = decoded(recordFrames { frame("Danger") { code(file) } }.single())

        assertThat(html, containsSubstring("&lt;script&gt;"))
        assertThat(html, containsSubstring("&lt;/script&gt;"))
    }

    @Test
    fun `image section embeds the file bytes as a base64 data URI`(@TempDir dir: File) {
        val file = File(dir, "pic.png").apply { writeBytes(tinyPng) }
        val expectedBase64 = Base64.getEncoder().encodeToString(tinyPng)

        val html = decoded(recordFrames { frame("Pic") { image(file) } }.single())

        assertThat(html, containsSubstring("data:image/png;base64,$expectedBase64"))
    }

    @Test
    fun `image section infers the MIME type from the file extension`(@TempDir dir: File) {
        val jpg = File(dir, "shot.jpg").apply { writeBytes(tinyPng) }
        val svg = File(dir, "icon.svg").apply { writeText("<svg/>") }

        val jpgHtml = decoded(recordFrames { frame("Shot") { image(jpg) } }.single())
        val svgHtml = decoded(recordFrames { frame("Icon") { image(svg) } }.single())

        assertThat(jpgHtml, containsSubstring("data:image/jpeg;base64,"))
        assertThat(svgHtml, containsSubstring("data:image/svg+xml;base64,"))
    }
}
