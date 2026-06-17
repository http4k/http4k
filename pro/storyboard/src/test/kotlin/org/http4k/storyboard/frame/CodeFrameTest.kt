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
import org.http4k.storyboard.recordFrames
import org.http4k.storyboard.util.gzipBase64Decode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CodeFrameTest {

    private fun decoded(frame: StoryFrame): String = frame.dom.gzipBase64Decode()

    @Test
    fun `code records a frame at Context level by default`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("fun greet() = \"hello\"") }

        val frame = recordFrames { code("The greeter", file) }.single()

        assertThat(frame.title, equalTo("The greeter"))
        assertThat(frame.notes, equalTo(""))
        assertThat(frame.level, equalTo(Context))
        assertThat(decoded(frame), containsSubstring("class=\"language-kotlin\""))
        assertThat(decoded(frame), containsSubstring("fun greet() = \"hello\""))
    }

    @Test
    fun `code reads the entire file when lines is null`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("line one\nline two\nline three") }

        val html = decoded(recordFrames { code("All", file) }.single())

        assertThat(html, containsSubstring("line one\nline two\nline three"))
    }

    @Test
    fun `code snips the given line range using 1-based inclusive bounds`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("one\ntwo\nthree\nfour\nfive") }

        val html = decoded(recordFrames { code("Middle", file, lines = 2..4) }.single())

        assertThat(html, containsSubstring("two\nthree\nfour"))
    }

    @Test
    fun `code infers language from file extension`(@TempDir dir: File) {
        val file = File(dir, "query.sql").apply { writeText("SELECT 1") }

        val html = decoded(recordFrames { code("Query", file) }.single())

        assertThat(html, containsSubstring("class=\"language-sql\""))
    }

    @Test
    fun `code respects an explicit language override`(@TempDir dir: File) {
        val file = File(dir, "config.txt").apply { writeText("a = 1") }

        val html = decoded(recordFrames { code("Config", file, language = "toml") }.single())

        assertThat(html, containsSubstring("class=\"language-toml\""))
    }

    @Test
    fun `code escapes HTML in the source so script tags survive as text`(@TempDir dir: File) {
        val file = File(dir, "danger.js").apply { writeText("<script>alert('x')</script>") }

        val html = decoded(recordFrames { code("Danger", file) }.single())

        assertThat(html, containsSubstring("&lt;script&gt;"))
        assertThat(html, containsSubstring("&lt;/script&gt;"))
    }
}
