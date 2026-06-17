package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.render.wrapAsHtmlDoc
import org.http4k.storyboard.util.gzipBase64Encode

/**
 * Capture an HTML frame. [content] may be a fragment or a full `<!DOCTYPE html>`
 * document — fragments are wrapped in a minimal page that loads Prism + Mermaid CDNs,
 * so `<pre class="mermaid">…` diagrams and code samples render without extra setup.
 */
fun Storyboard.html(title: String, content: String, notes: String = "", level: Level = Context) {
    capture(StoryFrame(title, notes, wrapAsHtmlDoc(content).gzipBase64Encode(), level))
}
