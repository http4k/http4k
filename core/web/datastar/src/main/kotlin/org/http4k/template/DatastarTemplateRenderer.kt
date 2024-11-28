package org.http4k.template

import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.Fragment
import org.http4k.datastar.MergeMode
import org.http4k.datastar.Selector
import org.http4k.datastar.SettleDuration

/**
 * Renderer which converts a ViewModel into a DatastarEvent.MergeFragments event
 */
class DatastarTemplateRenderer(private val renderer: TemplateRenderer) {
    operator fun invoke(
        vararg views: ViewModel,
        mergeMode: MergeMode = MergeMode.morph,
        selector: Selector? = null,
        useViewTransition: Boolean = false,
        settleDuration: SettleDuration? = SettleDuration.DEFAULT,
        id: String? = null,
    ) = DatastarEvent.MergeFragments(
        views.map { Fragment.of(renderer(it).stripNewLines()) },
        mergeMode,
        selector,
        useViewTransition,
        settleDuration,
        id
    )

    private fun String.stripNewLines() = replace("\n", "")
}
