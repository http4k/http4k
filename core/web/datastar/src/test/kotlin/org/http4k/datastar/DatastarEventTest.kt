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
