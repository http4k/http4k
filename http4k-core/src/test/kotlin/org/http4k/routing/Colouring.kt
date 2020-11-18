package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.EscapeMode.Ansi
import org.http4k.routing.EscapeMode.None
import org.http4k.routing.EscapeMode.Pseudo
import org.junit.jupiter.api.Test

class ColouringTest {

    @Test
    fun `can print with full ansi style`() {
        val style = TextStyle(ForegroundColour.Red, BackgroundColour.Blue, Variation.Underline)
        assertThat("test".styled(style), equalTo(ForegroundColour.Red.ansi + BackgroundColour.Blue.ansi + Variation.Underline.ansi + "test" + reset))
    }

    @Test
    fun `can print with style disabled`() {
        val style = TextStyle(ForegroundColour.Red, BackgroundColour.Blue, Variation.Underline)
        assertThat("test".styled(style, None), equalTo("test"))
    }

    @Test
    fun `can print with limited pseudo escaping`() {
        val style = TextStyle(ForegroundColour.Red, BackgroundColour.Blue, Variation.Underline)
        assertThat("test".styled(style, Pseudo), equalTo("_[red]test[red]_"))
    }

    @Test
    fun `default style is ignored`() {
        val style = TextStyle()
        assertThat("test".styled(style, None), equalTo("test"))
        assertThat("test".styled(style, Pseudo), equalTo("test"))
        assertThat("test".styled(style, Ansi), equalTo("test$reset"))
    }
}

fun String.styled(style: TextStyle, escapeMode: EscapeMode = Ansi) = escapeMode(this, style)

enum class ForegroundColour(val ansi: String) {
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

enum class BackgroundColour(val ansi: String) {
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

fun String.strikethrough() = "${Variation.Strikethrough.ansi}.value.orEmpty()}$this$reset"

enum class Variation(val ansi: String) {
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

enum class EscapeMode(private val converter: (String, TextStyle) -> String) {
    Ansi({ text, style -> "${style.foregroundColour.ansi}${style.backgroundColour.ansi}${style.variation.ansi}$text$reset" }),
    Pseudo({ text, style ->
        style.variation.pseudo() + style.foregroundColour.pseudo() + text + style.foregroundColour.pseudo() + style.variation.pseudo()
    }),
    None({ text, _ -> text });

    operator fun invoke(text: String, style: TextStyle) = converter(text, style)
}

private fun ForegroundColour.pseudo() = if (this == ForegroundColour.Default) "" else "[${name.toLowerCase()}]"

private fun Variation.pseudo(): String = when (this) {
    Variation.Bold -> "*"
    Variation.Italic -> "_"
    Variation.BoldItalic -> "**"
    Variation.Underline -> "_"
    Variation.Strikethrough -> "~"
    Variation.Default -> ""
}
