/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.layout.page

import org.http4k.storyboard.Chapter
import org.http4k.storyboard.Story
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryLayout
import org.http4k.storyboard.Theme
import org.http4k.storyboard.render.defaultLevel
import org.http4k.storyboard.render.flatten
import org.http4k.storyboard.util.StoryboardTemplates

class Page(private val theme: Theme = Theme.Http4k) : StoryLayout {
    override fun render(story: Story): String = StoryboardTemplates()(story.toPageView(theme))
}

private fun Story.toPageView(theme: Theme): PageView {
    val allFrames = flatten().map { it.frame }
    return PageView(
        theme = theme,
        pageTitle = "Storyboard: $title",
        heading = title,
        series = series,
        defaultMode = allFrames.defaultLevel().name.lowercase(),
        sections = chapters.map { it.toSection(depth = 1) }
    )
}

private fun Chapter.toSection(depth: Int): SectionView = SectionView(
    title = title,
    depth = depth,
    headingLevel = "h${(depth + 1).coerceAtMost(6)}",
    frames = frames.map { it.toPageFrameView() },
    children = children.map { it.toSection(depth + 1) }
)

private fun StoryFrame.toPageFrameView(): PageFrameView = PageFrameView(
    title = title,
    notes = notes,
    type = type,
    level = level.name,
    dom = dom
)
