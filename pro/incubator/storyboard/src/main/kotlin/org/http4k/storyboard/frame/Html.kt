package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame

/** Hand-authored HTML content (splash cards, Mermaid diagrams, prose). Wrapped with Prism/Mermaid CDNs for the slideshow. */
data class Html(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: StoryFrame.Level
) : StoryFrame
