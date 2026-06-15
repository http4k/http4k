package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame

/** DOM snapshot of a page driven by a `StoryboardWebDriver` — the bread-and-butter capture variant. */
data class WebDriverCapture(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: StoryFrame.Level
) : StoryFrame
