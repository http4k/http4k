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
import org.http4k.storyboard.util.StoryboardMoshi
import org.http4k.storyboard.util.StoryboardTemplates

class Page(private val theme: Theme = Theme.Http4k) : StoryLayout {
    override fun render(story: Story): String = StoryboardTemplates()(story.toPageView(theme))
}

private fun Story.toPageView(theme: Theme): PageView {
    val frames = flatten().map { it.frame }
    val counter = IntCounter()
    return PageView(
        theme = theme,
        pageTitle = "Storyboard: $title",
        heading = title,
        series = series,
        defaultMode = frames.defaultLevel().name.lowercase(),
        sections = chapters.map { it.toSection(depth = 1, counter) },
        framesJson = StoryboardMoshi.asFormatString(frames).replace("</", "<\\/")
    )
}

private class IntCounter { var n = 0; fun next() = n++ }

private fun Chapter.toSection(depth: Int, counter: IntCounter): SectionView = SectionView(
    title = title,
    depth = depth,
    headingLevel = "h${(depth + 1).coerceAtMost(6)}",
    frames = frames.map { it.toPageFrameView(counter.next()) },
    children = children.map { it.toSection(depth + 1, counter) }
)

private fun StoryFrame.toPageFrameView(index: Int): PageFrameView = PageFrameView(
    index = index,
    title = title,
    notes = notes,
    level = level.name
)
