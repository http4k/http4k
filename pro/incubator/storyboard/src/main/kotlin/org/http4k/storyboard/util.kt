/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

internal fun snip(text: String, lines: IntRange): String {
    val all = text.split('\n')
    val from = (lines.first - 1).coerceAtLeast(0)
    val to = lines.last.coerceAtMost(all.size)
    return all.subList(from, to).joinToString("\n")
}
