package org.http4k.storyboard.frame

import org.http4k.base64Encode
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.render.wrapAsHtmlDoc

/**
 * Capture an [HtmlFrame] frame. [content] may be a fragment or a full `<!DOCTYPE html>`
 * document — fragments are wrapped in a minimal page that loads Prism + Mermaid CDNs,
 * so `<pre class="mermaid">…` diagrams and code samples render without extra setup.
 */
fun Storyboard.html(title: String, content: String, notes: String = "", level: Level = Context) {
    captureFrame(HtmlFrame(title, notes, wrapAsHtmlDoc(content).base64Encode(), level))
}

/** Hand-authored HTML content (splash cards, Mermaid diagrams, prose). Wrapped with Prism/Mermaid CDNs for the slideshow. */
data class HtmlFrame(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: Level
) : StoryFrame
