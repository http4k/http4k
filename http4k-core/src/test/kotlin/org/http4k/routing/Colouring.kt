package org.http4k.routing

fun String.coloured(foregroundColour: ForegroundColour, backgroundColour: BackgroundColour? = null) =
    "${foregroundColour.value}${backgroundColour?.value.orEmpty()}$this$reset"

enum class ForegroundColour(val value: String) {
    Black("\u001B[30m"),
    Red("\u001B[31m"),
    Green("\u001B[32m"),
    Yellow("\u001B[33m"),
    Blue("\u001B[34m"),
    Purple("\u001B[35m"),
    Cyan("\u001B[36m"),
    White("\u001B[37m")
}

enum class BackgroundColour(val value: String) {
    Red("\u001B[41m"),
    Green("\u001B[42m"),
    Yellow("\u001B[43m"),
    Blue("\u001B[44m"),
    Purple("\u001B[45m"),
    Cyan("\u001B[46m"),
    White("\u001B[47m"),
    Black("\u001B[40m"),
}

private const val reset = "\u001B[0m"
