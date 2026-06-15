/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.base64Encode
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.frame.Code
import org.http4k.storyboard.frame.Html
import org.http4k.storyboard.frame.Image
import org.http4k.storyboard.render.escapeHtml
import org.http4k.storyboard.render.languageFor
import org.http4k.storyboard.render.mimeTypeFor
import org.http4k.storyboard.render.wrapAsHtmlDoc
import java.io.File
import java.util.Base64

/**
 * Capture an [Html] frame. [content] may be a fragment or a full `<!DOCTYPE html>`
 * document — fragments are wrapped in a minimal page that loads Prism + Mermaid CDNs,
 * so `<pre class="mermaid">…` diagrams and code samples render without extra setup.
 */
fun Storyboard.html(title: String, content: String, notes: String = "", level: Level = Context) {
    captureFrame(Html(title, notes, wrapAsHtmlDoc(content).base64Encode(), level))
}

/**
 * Capture a [Code] frame by reading [file] from disk. [lines] is a 1-based inclusive
 * range for excerpting; [language] overrides the language detected from the file
 * extension. Source text is HTML-escaped and wrapped with a Prism-enabled doc so the
 * code is syntax-highlighted in both renderers.
 */
fun Storyboard.code(
    title: String,
    file: File,
    lines: IntRange? = null,
    language: String? = null,
    notes: String = "",
    level: Level = Context
) {
    val raw = file.readText()
    val source = lines?.let { snip(raw, it) } ?: raw
    val lang = language ?: languageFor(file.extension)
    val body = """<pre><code class="language-$lang">${escapeHtml(source)}</code></pre>"""
    captureFrame(Code(title, notes, wrapAsHtmlDoc(body).base64Encode(), lang, source, level))
}

/**
 * Capture an [Image] frame by reading [file] from disk. The bytes are embedded as a
 * base64 `data:` URI inside an `<img>` tag, so the frame is fully self-contained — no
 * external asset references in the rendered story. Defaults to [Story] level since
 * images are typically headline content (diagrams, screenshots).
 */
fun Storyboard.image(title: String, file: File, notes: String = "", level: Level = Story) {
    val dataUri = "data:${mimeTypeFor(file.extension)};base64,${Base64.getEncoder().encodeToString(file.readBytes())}"
    val body = """<img src="$dataUri" alt="${escapeHtml(title)}" style="max-width:100%;height:auto">"""
    captureFrame(Image(title, notes, wrapAsHtmlDoc(body).base64Encode(), level))
}

/** Pass-through to [Storyboard.chapter] so test code can write `driver.chapter("...") {}` without reaching into `driver.storyboard`. */
fun StoryboardWebDriver.chapter(name: String, block: () -> Unit) = storyboard.chapter(name, block)

/** Pass-through to [Storyboard.html] — see that function for behaviour. */
fun StoryboardWebDriver.html(title: String, content: String, notes: String = "", level: Level = Context) =
    storyboard.html(title, content, notes, level)

/** Pass-through to [Storyboard.code] — see that function for behaviour. */
fun StoryboardWebDriver.code(
    title: String,
    file: File,
    lines: IntRange? = null,
    language: String? = null,
    notes: String = "",
    level: Level = Context
) = storyboard.code(title, file, lines, language, notes, level)

/** Pass-through to [Storyboard.image] — see that function for behaviour. */
fun StoryboardWebDriver.image(title: String, file: File, notes: String = "", level: Level = Story) =
    storyboard.image(title, file, notes, level)

private fun snip(text: String, lines: IntRange): String {
    val all = text.split('\n')
    val from = (lines.first - 1).coerceAtLeast(0)
    val to = lines.last.coerceAtMost(all.size)
    return all.subList(from, to).joinToString("\n")
}
