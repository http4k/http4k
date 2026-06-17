package org.http4k.storyboard.frame

import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.render.escapeHtml
import org.http4k.storyboard.render.languageFor
import org.http4k.storyboard.render.wrapAsHtmlDoc
import org.http4k.storyboard.util.gzipBase64Encode
import java.io.File

/**
 * Capture a code frame by reading [file] from disk. [lines] is a 1-based inclusive
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
    val source = lines?.let {
        val all = raw.split('\n')
        all.subList(
            (it.first - 1).coerceAtLeast(0),
            it.last.coerceAtMost(all.size)
        ).joinToString("\n")
    } ?: raw
    val lang = language ?: languageFor(file.extension)
    val body = """<pre><code class="language-$lang">${escapeHtml(source)}</code></pre>"""
    capture(StoryFrame(title, notes, wrapAsHtmlDoc(body).gzipBase64Encode(), level))
}
