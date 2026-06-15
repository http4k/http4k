package org.http4k.storyboard.frame

import org.http4k.base64Encode
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.render.escapeHtml
import org.http4k.storyboard.render.languageFor
import org.http4k.storyboard.render.wrapAsHtmlDoc
import org.http4k.storyboard.snip
import java.io.File

/**
 * Capture a [CodeFrame] frame by reading [file] from disk. [lines] is a 1-based inclusive
 * range for excerpting; [language] overrides the language detected from the file
 * extension. Source text is HTML-escaped and wrapped with a Prism-enabled doc so the
 * code is syntax-highlighted in both layouts.
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
    captureFrame(CodeFrame(title, notes, wrapAsHtmlDoc(body).base64Encode(), level))
}
/** Syntax-highlighted source code excerpt — [dom] is a Prism-enabled HTML doc wrapping the snippet. */
data class CodeFrame(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: Level
) : StoryFrame
