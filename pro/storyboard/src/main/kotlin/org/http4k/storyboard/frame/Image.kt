/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import org.http4k.core.MimeTypes
import org.http4k.storyboard.render.escapeHtml
import java.io.File
import java.util.Base64

/** Append an inline image section. The image file is embedded as a base64 data URI. */
fun FrameBuilder.image(file: File, alt: String = file.name) = section(Section(imageBody(file, alt)))

internal fun imageBody(file: File, alt: String): String {
    val dataUri =
        "data:${MimeTypes().forFile(file.name).value};base64,${Base64.getEncoder().encodeToString(file.readBytes())}"
    return """<img src="$dataUri" alt="${escapeHtml(alt)}" style="max-width:100%;height:auto">"""
}
