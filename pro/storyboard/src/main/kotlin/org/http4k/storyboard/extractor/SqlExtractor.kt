/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.extractor

import org.http4k.storyboard.EventContext
import org.http4k.storyboard.FrameExtractor
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.render.escapeHtml
import org.http4k.storyboard.render.wrapAsHtmlDoc
import org.http4k.storyboard.util.gzipBase64Encode

object SqlExtractor : FrameExtractor {
    override operator fun invoke(input: EventContext): StoryFrame? {
        val attrs = input.event.attributes
        val sql = attrs["db.statement"] ?: attrs["db.query.text"] ?: return null
        val system = attrs["db.system"] ?: attrs["db.system.name"] ?: "sql"
        val operation = attrs["db.operation"]
            ?: attrs["db.operation.name"]
            ?: sql.trim().substringBefore(' ').uppercase()
        val body = """<pre><code class="language-sql">${escapeHtml(sql)}</code></pre>"""
        return StoryFrame(
            title = "$system $operation",
            notes = "",
            dom = wrapAsHtmlDoc(body).gzipBase64Encode(),
            level = Detail
        )
    }
}
