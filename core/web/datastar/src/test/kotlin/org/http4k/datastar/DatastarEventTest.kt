package org.http4k.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test

class DatastarEventTest {

    @Test
    fun `merge fragment to event`() {
        assertThat(
            DatastarEvent.MergeFragments(Fragment.of("foo"), Fragment.of("bar")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-merge-fragments",
                    "fragments foo\nfragments bar\nsettleDuration 300\nmergeMode morph\nuseViewTransition false",
                    null
                )
            )
        )
    }

    @Test
    fun `merge signals to event`() {
        assertThat(
            DatastarEvent.MergeSignals(Signal.of("foo"), Signal.of("bar")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-merge-signals",
                    "signals foo\nsignals bar\nonlyIfMissing false",
                    null
                )
            )
        )
    }

    @Test
    fun `execute script to event`() {
        assertThat(
            DatastarEvent.ExecuteScript(Script.of("foo"), true, listOf("foo" to "bar", "foo2" to "bar2")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-execute-script",
                    "script foo\nautoRemove true\nattributes foo bar\nattributes foo2 bar2",
                    null
                )
            )
        )
    }

    @Test
    fun `remove fragments to event`() {
        assertThat(
            DatastarEvent.RemoveFragments(Selector.of("#foo")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-remove-fragments",
                    "selector #foo",
                    null
                )
            )
        )
    }

    @Test
    fun `remove signals to event`() {
        assertThat(
            DatastarEvent.RemoveSignals(SignalPath.of("foo"), SignalPath.of("bar")).toSseEvent(),
            equalTo(
                SseMessage.Event(
                    "datastar-remove-signals",
                    "paths foo\npaths bar",
                    null
                )
            )
        )
    }
}
