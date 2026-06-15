/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.layout.slideshow

import org.http4k.storyboard.Story
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryLayout
import org.http4k.storyboard.Theme
import org.http4k.storyboard.render.defaultLevel
import org.http4k.storyboard.render.flatten
import org.http4k.storyboard.util.StoryboardMoshi
import org.http4k.storyboard.util.StoryboardTemplates

class Slideshow(private val theme: Theme = Theme.Http4k) : StoryLayout {
    override fun render(story: Story): String = StoryboardTemplates()(story.toView(theme))
}

private fun Story.toView(theme: Theme): SlideshowView {
    val flat = flatten()
    val frames = flat.map { it.frame }
    // Drop the root chapter from the path: it carries the story title (already shown as the heading).
    val paths = flat.map { it.path.drop(1) }
    return SlideshowView(
        theme = theme,
        pageTitle = "Storyboard: $title",
        heading = title,
        series = series,
        tiles = buildTiles(frames, paths),
        framesJson = StoryboardMoshi.asFormatString(frames).replace("</", "<\\/"),
        chapterPathsJson = StoryboardMoshi.asFormatString(paths).replace("</", "<\\/"),
        defaultMode = frames.defaultLevel().name.lowercase()
    )
}

private fun buildTiles(frames: List<StoryFrame>, paths: List<List<String>>): List<TileView> {
    val hasMultipleChapters = paths.toSet().size > 1
    var prev = emptyList<String>()
    return frames.mapIndexed { i, f ->
        val path = paths.getOrElse(i) { emptyList() }
        val boundary = when {
            !hasMultipleChapters -> null
            path == prev || path.isEmpty() -> null
            else -> ChapterBoundaryView(title = path.last(), depth = (path.size - 1).coerceAtLeast(0))
        }
        prev = path
        TileView(i, f.title, f.level.name, boundary)
    }
}
