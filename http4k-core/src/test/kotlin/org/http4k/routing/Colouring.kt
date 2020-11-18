package org.http4k.routing

fun String.coloured(foregroundColour: ForegroundColour, backgroundColour: BackgroundColour? = null) =
    "${foregroundColour.value}${backgroundColour?.value.orEmpty()}$this$reset"

fun String.styled(style: TextStyle) =
    "${style.foregroundColour.value}${style.backgroundColour.value}${style.variation.value}$this$reset"

enum class ForegroundColour(val value: String) {
    Black("\u001B[30m"),
    Red("\u001B[31m"),
    Green("\u001B[32m"),
    Yellow("\u001B[33m"),
    Blue("\u001B[34m"),
    Purple("\u001B[35m"),
    Cyan("\u001B[36m"),
    White("\u001B[37m"),
    Default("")
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
    Default("")
}

fun String.strikethrough() = "${Variation.Strikethrough.value}.value.orEmpty()}$this$reset"

enum class Variation(val value: String) {
    Bold("\u001B[1m"),
    Italic("\u001B[3m"),
    BoldItalic("\u001B[3m"),
    Underline("\u001B[4m"),
    Strikethrough("\u001B[9m"),
    Default("")
}

private const val reset = "\u001B[0m"

data class TextStyle(
    val foregroundColour: ForegroundColour = ForegroundColour.Default,
    val backgroundColour: BackgroundColour = BackgroundColour.Default,
    val variation: Variation = Variation.Default,
)
