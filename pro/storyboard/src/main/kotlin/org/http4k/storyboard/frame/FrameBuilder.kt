/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.render.wrapAsHtmlDoc
import org.http4k.storyboard.util.gzipBase64Encode

/**
 * Capture a frame composed of one or more ordered sections.
 */
fun Storyboard.frame(
    title: String,
    level: Level = Context,
    block: FrameBuilder.() -> Unit
) {
    val builder = FrameBuilder().apply(block)
    capture(StoryFrame(title, builder.notes, wrapAsHtmlDoc(builder.assemble()).gzipBase64Encode(), level))
}

class FrameBuilder internal constructor() {
    /** Secondary text rendered next to / under the frame. */
    var notes = ""

    private val sections = mutableListOf<Section>()

    /** Append a pre-built section. Escape hatch for custom section content. */
    fun section(section: Section) {
        sections += section
    }

    internal fun assemble(): String = sectionStyle + sections.joinToString("") {
        """<section class="frame-section">${it.body}</section>"""
    }
}

data class Section(val body: String)

private const val sectionStyle =
    """<style>.frame-section + .frame-section { margin-top: 1rem; }</style>"""
