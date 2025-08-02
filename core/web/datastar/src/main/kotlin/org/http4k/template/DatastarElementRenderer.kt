package org.http4k.template

import org.http4k.datastar.Element

/**
 * Renderer which converts a ViewModel into Datastar Fragments
 */
class DatastarElementRenderer(private val renderer: TemplateRenderer) {
    operator fun invoke(vararg views: ViewModel) = views.map { Element.of(renderer(it).stripNewLines()) }

    private fun String.stripNewLines() = replace("\r\n", "").replace("\n", "")
}
