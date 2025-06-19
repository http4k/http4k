package org.http4k.ai.llm.chat

import org.http4k.ai.llm.model.Content

fun List<Content>.consolidate() =
    map {
        when (it) {
            is Content.Text -> Content.Text(it.text.filter { it.isLetterOrDigit() }.lowercase())
            else -> it
        }
    }.filter { it !is Content.Text || it.text.isNotEmpty() }
