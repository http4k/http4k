package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame

/**
 * Syntax-highlighted source code excerpt. [source] holds the raw text (re-styled by the
 * Flat renderer for inline display), [language] is a Prism language tag (`kotlin`, `sql`, …),
 * and [dom] is the pre-wrapped iframe payload used by the Slideshow.
 */
data class Code(
    override val title: String,
    override val notes: String,
    override val dom: String,
    val language: String,
    val source: String,
    override val level: StoryFrame.Level = StoryFrame.Level.Context
) : StoryFrame
