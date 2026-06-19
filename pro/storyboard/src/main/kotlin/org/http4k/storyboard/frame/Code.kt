/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import org.http4k.storyboard.render.escapeHtml
import org.http4k.storyboard.render.languageFor
import java.io.File

/** Append a Prism-highlighted code section. */
fun FrameBuilder.code(file: File, lines: IntRange? = null, language: String? = null) =
    section(Section(codeBody(file, lines, language)))

internal fun codeBody(file: File, lines: IntRange?, language: String?): String {
    val raw = file.readText()
    val source = lines?.let {
        val all = raw.split('\n')
        all.subList((it.first - 1).coerceAtLeast(0), it.last.coerceAtMost(all.size)).joinToString("\n")
    } ?: raw
    val lang = language ?: languageFor(file.extension)
    return """<pre><code class="language-$lang">${escapeHtml(source)}</code></pre>"""
}
