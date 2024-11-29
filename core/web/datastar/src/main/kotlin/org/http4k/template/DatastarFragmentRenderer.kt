package org.http4k.template

import org.http4k.datastar.Fragment

/**
 * Renderer which converts a ViewModel into Datastar Fragments
 */
class DatastarFragmentRenderer(private val renderer: TemplateRenderer) {
    operator fun invoke(vararg views: ViewModel) = views.map { Fragment.of(renderer(it).stripNewLines()) }

    private fun String.stripNewLines() = replace("\n", "")
}
