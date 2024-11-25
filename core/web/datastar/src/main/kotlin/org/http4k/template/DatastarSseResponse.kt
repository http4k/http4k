package org.http4k.template

import org.http4k.core.Headers
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.Fragment
import org.http4k.datastar.MergeMode
import org.http4k.datastar.MergeMode.morph
import org.http4k.datastar.Selector
import org.http4k.datastar.SettleDuration
import org.http4k.datastar.SettleDuration.Companion.DEFAULT
import org.http4k.sse.SseResponse

/**
 * Custom SseHandler for Datastar when used with TemplateRenderers
 */
interface DatastarSseResponse {
    val status: Status
    val headers: Headers

    /**
     * Render the response using the provided TemplateRenderer
     */
    operator fun invoke(renderer: TemplateRenderer): SseResponse

    /**
     * Render the views into the correct format for the Datastar event using the provided TemplateRenderer
     */
    class MergeFragments(
        private vararg val views: ViewModel,
        override val status: Status = OK,
        override val headers: Headers = emptyList(),
        private val mergeMode: MergeMode = morph,
        private val selector: Selector? = null,
        private val useViewTransition: Boolean = false,
        private val settleDuration: SettleDuration? = DEFAULT,
        private val id: String? = null,
        private val close: Boolean = true
    ) : DatastarSseResponse {
        override fun invoke(renderer: TemplateRenderer) = SseResponse(status, headers) {
            it.send(
                DatastarEvent.MergeFragments(
                    views.map { Fragment.of(renderer(it).stripNewLines()) },
                    mergeMode,
                    selector,
                    useViewTransition,
                    settleDuration,
                    id
                ).toSseEvent()
            )
            if (close) it.close()
        }

        private fun String.stripNewLines() = replace("\n", "")
    }

    data class Error(override val status: Status, override val headers: Headers = emptyList()) : DatastarSseResponse {
        override fun invoke(renderer: TemplateRenderer) = SseResponse(status, headers) { it.close() }
    }
}
