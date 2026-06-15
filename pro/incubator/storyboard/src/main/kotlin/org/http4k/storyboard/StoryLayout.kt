/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

/**
 * Page-level arrangement of a [Story]'s frames into an HTML document. Built-in
 * layouts: `Slideshow` (one frame at a time with navigation) and `Page` (all frames
 * stacked into a single scrollable page). Each frame's own visual content is rendered
 * inside an iframe via [StoryFrame.dom] — layout chooses *how the frames are arranged*,
 * not how each frame draws itself.
 */
fun interface StoryLayout {
    fun render(story: Story): String
}
