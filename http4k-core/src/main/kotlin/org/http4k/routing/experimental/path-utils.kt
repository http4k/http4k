package org.http4k.routing.experimental


fun String.pathJoin(suffix: String) = "${this.withoutTrailingSlash()}/${suffix.withoutLeadingSlash()}"

fun String.orIndexFile() =
    if (isEmpty() || endsWith("/"))
        pathJoin("index.html")
    else
        this

fun String.withoutTrailingSlash() = if (this.endsWith("/")) this.dropLast(1) else this

fun String.withoutLeadingSlash() = if (this.startsWith("/")) this.drop(1) else this
