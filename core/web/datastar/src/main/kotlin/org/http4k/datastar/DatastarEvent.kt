package org.http4k.datastar

import org.http4k.datastar.Element.Companion.of
import org.http4k.datastar.MorphMode.outer
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage.Event

sealed class DatastarEvent(val name: String, val data: List<String>, open val id: SseEventId?) {

    fun toSseEvent() = Event(name, data.joinToString("\n"), id)

    /**
     * The datastar-patch-elements event is used to merge HTML elements into the DOM.
     * The elements line should be a valid HTML syntax (tags can span multiple lines).
     * The selector line should be a valid CSS selector.
     * The mode line determines how the element are merged into the DOM.
     * The useViewTransition line determines whether to use a view transition.
     */
    data class PatchElements(
        val elements: List<Element>,
        val mode: MorphMode = outer,
        val selector: Selector? = null,
        val useViewTransition: Boolean = false,
        override val id: SseEventId? = null,
    ) : DatastarEvent(
        "datastar-patch-elements",
        run {
            val nullable = listOfNotNull(
                selector?.value?.let { "selector $it" },
            )
            val other = listOf(
                "mode $mode",
                "useViewTransition $useViewTransition"
            )
            elements.map { "elements $it" } + nullable + other
        },
        id
    ) {
        constructor(
            vararg element: Element,
            morphMode: MorphMode = outer,
            selector: Selector? = null,
            useViewTransition: Boolean = false,
            id: SseEventId? = null,
        ) : this(element.toList(), morphMode, selector, useViewTransition, id)

        constructor(
            vararg element: String,
            morphMode: MorphMode = outer,
            selector: Selector? = null,
            useViewTransition: Boolean = false,
            id: SseEventId? = null,
        ) : this(element.map { of(it) }.toList(), morphMode, selector, useViewTransition, id)
    }

    /**
     * The datastar-patch-signals event is used to update the store with new values.
     * The onlyIfMissing line determines whether to update the store with new values only if the key does not exist.
     * The signals lines should be a valid data-store attribute. These will get merged into the store.
     */
    data class PatchSignals(
        val signals: List<Signal>,
        val onlyIfMissing: Boolean? = false,
        override val id: SseEventId? = null,
    ) : DatastarEvent("datastar-patch-signals", run {
        signals.map { "signals $it" } + listOfNotNull(onlyIfMissing?.let { "onlyIfMissing $it" })
    }, id) {
        constructor(
            vararg signal: Signal,
            onlyIfMissing: Boolean? = false,
            id: SseEventId? = null,
        ) : this(signal.toList(), onlyIfMissing, id)
    }

    companion object {
        /**
         * Parse an event from an SSE Event.
         */
        fun from(event: Event) = with(event.data.split("\n")) {
            when (event.event) {
                "datastar-patch-elements" -> {
                    PatchElements(
                        data("elements", Element.Companion::of),
                        data("mode", MorphMode::valueOf).first(),
                        data("selector", Selector.Companion::of).firstOrNull(),
                        data("useViewTransition", String::toBoolean).firstOrNull() ?: false,
                        event.id
                    )
                }

                "datastar-patch-signals" -> {
                    PatchSignals(
                        data("signals", Signal.Companion::of),
                        data("onlyIfMissing", String::toBoolean).firstOrNull(),
                        event.id
                    )
                }

                else -> error("unknown event ${event.event}")
            }
        }

        private fun <T> List<String>.data(name: String, fn: (String) -> T): List<T> =
            filter { it.startsWith(name) }.map { it.removePrefix(name).trim() }.map(fn)
    }
}

