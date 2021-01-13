package org.http4k.routing.inspect

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.inspect.EscapeMode.Ansi
import org.http4k.routing.inspect.EscapeMode.None
import org.http4k.routing.inspect.EscapeMode.Pseudo
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

