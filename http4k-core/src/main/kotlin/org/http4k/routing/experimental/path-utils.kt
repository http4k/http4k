package org.http4k.routing.experimental

internal fun String.pathJoin(suffix: String) = when {
    this.isEmpty() -> suffix
    suffix.isEmpty() -> this
    else -> "${this.withoutTrailingSlash()}/${suffix.withoutLeadingSlash()}"
}

internal fun String.withoutTrailingSlash() = if (this.endsWith("/")) this.dropLast(1) else this

internal fun String.withoutLeadingSlash() = if (this.startsWith("/")) this.drop(1) else this

internal fun String.withLeadingSlash() = if (this.startsWith("/")) this else "/$this"