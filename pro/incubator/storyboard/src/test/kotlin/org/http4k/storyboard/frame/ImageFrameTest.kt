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
import org.http4k.storyboard.image
import org.http4k.storyboard.recordFrames
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Base64

class ImageFrameTest {

    // 1x1 transparent PNG
    private val tinyPng: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    )

    private fun decoded(frame: StoryFrame): String = String(Base64.getDecoder().decode(frame.dom))

    @Test
    fun `image records a frame at Story level by default`(@TempDir dir: File) {
        val file = File(dir, "logo.png").apply { writeBytes(tinyPng) }

        val frame = recordFrames { image("The logo", file) }.single() as Image

        assertThat(frame.title, equalTo("The logo"))
        assertThat(frame.notes, equalTo(""))
        assertThat(frame.level, equalTo(Story))
    }

    @Test
    fun `image captures at the specified level`(@TempDir dir: File) {
        val file = File(dir, "splash.png").apply { writeBytes(tinyPng) }

        val frame = recordFrames { image("Splash", file, level = Context) }.single() as Image

        assertThat(frame.level, equalTo(Context))
    }

    @Test
    fun `image embeds the file bytes as a base64 data URI in the dom`(@TempDir dir: File) {
        val file = File(dir, "pic.png").apply { writeBytes(tinyPng) }
        val expectedBase64 = Base64.getEncoder().encodeToString(tinyPng)

        val html = decoded(recordFrames { image("Pic", file) }.single())

        assertThat(html, containsSubstring("data:image/png;base64,$expectedBase64"))
    }

    @Test
    fun `image infers the MIME type from the file extension`(@TempDir dir: File) {
        val jpg = File(dir, "shot.jpg").apply { writeBytes(tinyPng) }
        val svg = File(dir, "icon.svg").apply { writeText("<svg/>") }

        val jpgHtml = decoded(recordFrames { image("Shot", jpg) }.single())
        val svgHtml = decoded(recordFrames { image("Icon", svg) }.single())

        assertThat(jpgHtml, containsSubstring("data:image/jpeg;base64,"))
        assertThat(svgHtml, containsSubstring("data:image/svg+xml;base64,"))
    }
}
