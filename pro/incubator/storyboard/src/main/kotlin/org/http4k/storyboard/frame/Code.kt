package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame

/** Syntax-highlighted source code excerpt — [dom] is a Prism-enabled HTML doc wrapping the snippet. */
data class Code(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: StoryFrame.Level
) : StoryFrame
