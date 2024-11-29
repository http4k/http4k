package org.http4k.datastar

import org.http4k.datastar.Fragment.Companion.of
import org.http4k.datastar.MergeMode.morph
import org.http4k.datastar.SettleDuration.Companion.DEFAULT
import org.http4k.sse.SseMessage.Event
import java.time.Duration

sealed class DatastarEvent(val name: String, val data: List<String>, open val id: String?) {

    fun toSseEvent() = Event(name, data.joinToString("\n"), id)

    /**
     * The datastar-merge-fragments event is used to merge HTML fragments into the DOM. The fragments
     * line should be a valid HTML fragment. The selector line should be a valid CSS selector. The mode
     * line determines how the fragments are merged into the DOM. The useViewTransition line determines
     * whether to use a view transition. The settleDuration line determines how long the transition
     * should take.
     */
    data class MergeFragments(
        val fragments: List<Fragment>,
        val mergeMode: MergeMode = morph,
        val selector: Selector? = null,
        val useViewTransition: Boolean = false,
        val settleDuration: SettleDuration? = DEFAULT,
        override val id: String? = null,
    ) : DatastarEvent(
        "datastar-merge-fragments",
        run {
            val nullable = listOfNotNull(
                selector?.value?.let { "selector $it" },
                settleDuration?.let { "settleDuration ${it.value.toMillis()}" }
            )
            val other = listOf(
                "mergeMode $mergeMode",
                "useViewTransition $useViewTransition"
            )
            fragments.map { "fragments $it" } + nullable + other
        },
        id
    ) {
        constructor(
            vararg fragment: Fragment,
            mergeMode: MergeMode = morph,
            selector: Selector? = null,
            useViewTransition: Boolean = false,
            settleDuration: SettleDuration? = DEFAULT,
            id: String? = null,
        ) : this(fragment.toList(), mergeMode, selector, useViewTransition, settleDuration, id)

        constructor(
            vararg fragment: String,
            mergeMode: MergeMode = morph,
            selector: Selector? = null,
            useViewTransition: Boolean = false,
            settleDuration: SettleDuration? = DEFAULT,
            id: String? = null,
        ) : this(fragment.map { of(it) }.toList(), mergeMode, selector, useViewTransition, settleDuration, id)
    }

    /**
     * The datastar-merge-signals event is used to update the store with new values. The onlyIfMissing
     * line determines whether to update the store with new values only if the key does not exist. The
     * signals line should be a valid data-store attribute. This will get merged into the store.
     */
    data class MergeSignals(
        val signals: List<Signal>,
        val onlyIfMissing: Boolean? = false,
        override val id: String? = null,
    ) : DatastarEvent("datastar-merge-signals", run {
        signals.map { "signals $it" } + listOfNotNull(onlyIfMissing?.let { "onlyIfMissing $it" })
    }, id) {
        constructor(
            vararg signal: Signal,
            onlyIfMissing: Boolean? = false,
            id: String? = null,
        ) : this(signal.toList(), onlyIfMissing, id)
    }

    /**
     * The datastar-remove-fragments event is used to remove HTML fragments that match the provided
     * selector from the DOM.
     */
    data class RemoveFragments(
        val selector: Selector,
        override val id: String? = null,
    ) : DatastarEvent("datastar-remove-fragments", listOf("selector $selector"), id)

    /**
     * The datastar-remove-signals event is used to remove signals that match the provided paths
     * from the store.
     */
    data class RemoveSignals(
        val paths: List<SignalPath>,
        override val id: String? = null,
    ) : DatastarEvent("datastar-remove-signals", paths.map { "paths $it" }, id) {
        constructor(
            vararg path: SignalPath,
            id: String? = null,
        ) : this(path.toList(), id)
    }

    /**
     * The datastar-execute-script event is used to execute JavaScript in the browser.
     * The autoRemove line determines whether to remove the script after execution.
     * Each attributes line adds an attribute (in the format name value) to the script element.
     * Each script line contains JavaScript to be executed by the browser.
     */
    data class ExecuteScript(
        val script: Script,
        val autoRemove: Boolean = true,
        val attributes: List<Pair<String, String>> = emptyList(),
        override val id: String? = null,
    ) : DatastarEvent("datastar-execute-script", run {
        listOf("script $script", "autoRemove $autoRemove") + attributes.map { "attributes ${it.first} ${it.second}" }
    }, id)

    companion object {
        /**
         * Parse an event from an SSE Event.
         */
        fun from(event: Event) = with(event.data.split("\n")) {
            when (event.event) {
                "datastar-merge-fragments" -> {
                    MergeFragments(
                        data("fragments", Fragment.Companion::of),
                        data("mergeMode", MergeMode::valueOf).first(),
                        data("selector", Selector.Companion::of).firstOrNull(),
                        data("useViewTransition", String::toBoolean).firstOrNull() ?: false,
                        data("settleDuration", { SettleDuration.of(Duration.ofMillis(it.toLong())) }).firstOrNull(),
                        event.id
                    )
                }

                "datastar-merge-signals" -> {
                    MergeSignals(
                        data("signals", Signal.Companion::of),
                        data("onlyIfMissing", String::toBoolean).firstOrNull(),
                        event.id
                    )
                }

                "datastar-remove-fragments" -> {
                    RemoveFragments(
                        data("selector", Selector.Companion::of).first(),
                        event.id
                    )
                }

                "datastar-remove-signals" -> {
                    RemoveSignals(
                        data("paths", SignalPath.Companion::of),
                        event.id
                    )
                }

                "datastar-execute-script" -> {
                    ExecuteScript(
                        data("script", Script.Companion::of).first(),
                        data("autoRemove", String::toBoolean).firstOrNull() ?: true,
                        data("attributes", { it: String -> it.split(" ").let { i -> i[0] to i[1] } }),
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

