/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame

/** A binary image (PNG/JPEG/SVG/…) embedded as a base64 data URI inside a wrapped HTML doc. */
data class Image(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: StoryFrame.Level
) : StoryFrame
