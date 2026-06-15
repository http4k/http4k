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
import org.http4k.storyboard.code
import org.http4k.storyboard.recordFrames
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Base64

class CodeFrameTest {

    private fun decoded(frame: StoryFrame): String = String(Base64.getDecoder().decode(frame.dom))

    @Test
    fun `code records a frame at Context level by default`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("fun greet() = \"hello\"") }

        val frame = recordFrames { it.code("The greeter", file) }.single() as Code

        assertThat(frame.title, equalTo("The greeter"))
        assertThat(frame.notes, equalTo(""))
        assertThat(frame.level, equalTo(Context))
        assertThat(frame.language, equalTo("kotlin"))
        assertThat(frame.source, equalTo("fun greet() = \"hello\""))
    }

    @Test
    fun `code reads the entire file when lines is null`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("line one\nline two\nline three") }

        val frame = recordFrames { it.code("All", file) }.single() as Code

        assertThat(frame.source, equalTo("line one\nline two\nline three"))
    }

    @Test
    fun `code snips the given line range using 1-based inclusive bounds`(@TempDir dir: File) {
        val file = File(dir, "Sample.kt").apply { writeText("one\ntwo\nthree\nfour\nfive") }

        val frame = recordFrames { it.code("Middle", file, lines = 2..4) }.single() as Code

        assertThat(frame.source, equalTo("two\nthree\nfour"))
    }

    @Test
    fun `code infers language from file extension`(@TempDir dir: File) {
        val file = File(dir, "query.sql").apply { writeText("SELECT 1") }

        val frame = recordFrames { it.code("Query", file) }.single() as Code

        assertThat(frame.language, equalTo("sql"))
    }

    @Test
    fun `code respects an explicit language override`(@TempDir dir: File) {
        val file = File(dir, "config.txt").apply { writeText("a = 1") }

        val frame = recordFrames { it.code("Config", file, language = "toml") }.single() as Code

        assertThat(frame.language, equalTo("toml"))
    }

    @Test
    fun `code escapes HTML in the source so script tags survive as text`(@TempDir dir: File) {
        val file = File(dir, "danger.js").apply { writeText("<script>alert('x')</script>") }

        val html = decoded(recordFrames { it.code("Danger", file) }.single())

        assertThat(html, containsSubstring("&lt;script&gt;"))
        assertThat(html, containsSubstring("&lt;/script&gt;"))
    }
}
