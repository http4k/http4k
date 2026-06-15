/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.render

import org.http4k.storyboard.Chapter
import org.http4k.storyboard.Story
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.StoryFrame.Level.Detail

fun Story.flatten(): List<FlatFrame> = chapters.flatMap { it.flatten(emptyList()) }

private fun Chapter.flatten(prefix: List<String>): List<FlatFrame> {
    val here = prefix + title
    return frames.map { FlatFrame(it, here) } + children.flatMap { it.flatten(here) }
}

internal fun List<StoryFrame>.defaultLevel(): Level = when {
    any { it.level == Level.Story } -> Level.Story
    any { it.level == Context } -> Context
    else -> Detail
}
