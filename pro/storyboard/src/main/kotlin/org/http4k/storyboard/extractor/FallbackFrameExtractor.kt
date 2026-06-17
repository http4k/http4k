/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.extractor

import org.http4k.storyboard.EventContext
import org.http4k.storyboard.FrameExtractor
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.util.StoryboardMoshi

object FallbackFrameExtractor : FrameExtractor {
    override fun invoke(input: EventContext): StoryFrame? {
        val json = input.event.attributes["storyboard.frame"] ?: return null
        return runCatching { StoryboardMoshi.asA<StoryFrame>(json) }.getOrNull()
    }
}
