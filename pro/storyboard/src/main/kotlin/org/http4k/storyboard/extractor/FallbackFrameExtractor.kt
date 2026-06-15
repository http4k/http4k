/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.extractor

import org.http4k.storyboard.EventContext
import org.http4k.storyboard.FrameExtractor
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.util.StoryboardMoshi

/**
 * Fallback for events the recorder itself emitted: reads the `storyboard.type`
 * (JVM FQN), loads the class reflectively and hands the JSON payload to Moshi
 * for rehydration. Any [StoryFrame] subclass on the classpath rehydrates for free.
 */
object FallbackFrameExtractor : FrameExtractor {
    override fun invoke(input: EventContext): StoryFrame? {
        val type = input.event.attributes["storyboard.type"] ?: return null
        val json = input.event.attributes["storyboard.frame"] ?: return null
        val kclass = runCatching { Class.forName(type).kotlin }.getOrNull() ?: return null
        return StoryboardMoshi.asA(json, kclass) as? StoryFrame
    }
}
