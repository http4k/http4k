/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import org.http4k.base64Encode
import org.http4k.core.MimeTypes
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.render.escapeHtml
import org.http4k.storyboard.render.wrapAsHtmlDoc
import java.io.File
import java.util.Base64

/**
 * Capture an [ImageFrame] frame by reading [file] from disk.
 */
fun Storyboard.image(title: String, file: File, notes: String = "", level: Level = Story) {
    val dataUri =
        "data:${MimeTypes().forFile(file.name).value};base64,${Base64.getEncoder().encodeToString(file.readBytes())}"
    val body = """<img src="$dataUri" alt="${escapeHtml(title)}" style="max-width:100%;height:auto">"""
    capture(ImageFrame(title, notes, wrapAsHtmlDoc(body).base64Encode(), level))
}

data class ImageFrame(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: Level
) : StoryFrame
