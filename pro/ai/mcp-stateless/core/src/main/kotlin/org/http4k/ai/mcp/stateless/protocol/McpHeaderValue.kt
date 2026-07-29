/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol

import java.util.Base64

private val sentinel = Regex("""^=\?base64\?(.*)\?=$""")

private val base64Alphabet = Regex("""^[A-Za-z0-9+/]*={0,2}$""")

fun decodeMcpHeaderValue(value: String): String? = when (val match = sentinel.matchEntire(value)) {
    null -> value
    else -> match.groupValues[1].decodeStrictBase64()
}

private fun String.decodeStrictBase64() = when {
    length % 4 != 0 || !base64Alphabet.matches(this) -> null
    else -> runCatching { String(Base64.getDecoder().decode(this), Charsets.UTF_8) }.getOrNull()
}
