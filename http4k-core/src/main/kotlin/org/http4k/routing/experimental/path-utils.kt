package org.http4k.routing.experimental


fun String.pathJoin(suffix: String) = "${this.withoutTrailingSlash()}/${suffix.withoutLeadingSlash()}"

fun String.withoutTrailingSlash() = if (this.endsWith("/")) this.dropLast(1) else this

fun String.withoutLeadingSlash() = if (this.startsWith("/")) this.drop(1) else this

fun String.withLeadingSlash() = if (this.startsWith("/")) this else "/$this"

fun String.withTrailingSlash() = if (this.endsWith("/")) this else "$this/"