/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.storyboard.util.StoryboardMoshi

/**
 * A single panel inside a [Chapter].
 *
 * @property title shown above the frame in both layouts
 * @property notes optional secondary text rendered next to / under the frame
 * @property dom gzip-then-base64 encoded HTML payload
 * @property level controls visibility under the Story/Context/Detail mode toggle
 * @property highlight controls CSS to highlight in the attached DOM
 * @property domAssets local assets referenced by the DOM, keyed by original URL
 */
data class StoryFrame(
    val title: String,
    val notes: String,
    val dom: String,
    val level: Level,
    val highlight: String? = null,
    val domAssets: Map<String, DomAsset> = mapOf()
) {
    fun toEventAttributes(): Map<String, String> = mapOf("storyboard.frame" to StoryboardMoshi.asFormatString(this))

    /**
     * Detail level for filtering frames via the Story/Context/Detail toggle
     * */
    enum class Level { Story, Context, Detail }
}

/**
 * A local asset captured alongside a [StoryFrame.dom] so it remains available when the captured
 * page is rendered offline inside an iframe.
 */
data class DomAsset(val mimeType: String, val content: String)
