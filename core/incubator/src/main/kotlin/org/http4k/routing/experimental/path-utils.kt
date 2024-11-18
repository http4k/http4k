package org.http4k.routing.experimental

internal fun String.pathJoin(suffix: String) = when {
    isEmpty() -> suffix
    suffix.isEmpty() -> this
    else -> "${withoutTrailingSlash()}/${suffix.withoutLeadingSlash()}"
}

internal fun String.withoutTrailingSlash() = if (endsWith("/")) dropLast(1) else this

internal fun String.withoutLeadingSlash() = if (startsWith("/")) drop(1) else this

internal fun String.withLeadingSlash() = if (startsWith("/")) this else "/$this"
