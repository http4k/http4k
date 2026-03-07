package org.http4k.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test

class DatastarEventTest {

    @Test
    fun `patch elements to event`() {
        assertThat(
            DatastarEvent.PatchElements(Element.of("foo"), Element.of("bar")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-patch-elements",
                    "elements foo\nelements bar",
                    null
                )
            )
        )
    }

    @Test
    fun `patch multi-line element splits into separate SSE data lines`() {
        assertThat(
            DatastarEvent.PatchElements(Element.of("<pre>\n  hello\n</pre>")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-patch-elements",
                    "elements <pre>\nelements   hello\nelements </pre>",
                    null
                )
            )
        )
    }

    @Test
    fun `round trips multi-line element`() {
        val original = DatastarEvent.PatchElements(Element.of("<pre>\n  hello\n</pre>"))
        assertThat(DatastarEvent.from(original.toSseEvent()), equalTo(original))
    }

    @Test
    fun `only include useViewTransition and mode in event if defaults are overridden`() {
        assertThat(
            DatastarEvent.PatchElements(Element.of("foo"), morphMode = MorphMode.replace, useViewTransition = true).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-patch-elements",
                    "elements foo\nmode replace\nuseViewTransition true",
                    null
                )
            )
        )
    }

    @Test
    fun `patch signals to event`() {
        assertThat(
            DatastarEvent.PatchSignals(Signal.of("foo"), Signal.of("bar")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-patch-signals",
                    "signals foo\nsignals bar\nonlyIfMissing false",
                    null
                )
            )
        )
    }
}
