/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.storyboard.util.StoryboardMoshi

/**
 * A single slide / inline panel inside a [Chapter].
 *
 * @property type short display tag used by the layouts
 * @property title shown above the frame in both layouts
 * @property notes optional secondary text rendered next to / under the frame
 * @property dom base64-encoded HTML payload
 * @property level controls visibility under the Story/Context/Detail mode toggle
 */
interface StoryFrame {
    val type: String get() = this::class.simpleName!!.lowercase()
    val title: String
    val notes: String
    val dom: String
    val level: Level

    /**
     * Serialises this frame into the OTel-event attribute
     */
    fun toEventAttributes(): Map<String, String> = mapOf(
        "storyboard.type" to this::class.java.name,
        "storyboard.frame" to StoryboardMoshi.asFormatString(this)
    )

    /**
     * Detail level for filtering frames via the Story/Context/Detail toggle
     * */
    enum class Level { Story, Context, Detail }
}
