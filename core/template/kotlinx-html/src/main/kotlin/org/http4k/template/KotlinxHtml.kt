package org.http4k.template

import kotlinx.html.TagConsumer
import kotlinx.html.stream.appendHTML

/**
 * A ViewModel which renders itself using the kotlinx-html DSL. Emit a single root tag for a full
 * page (html { .. }) or any other tag for a fragment (div { .. }).
 */
interface HtmlViewModel : ViewModel {
    fun TagConsumer<*>.render()
}

object KotlinxHtmlRenderer : TemplateRenderer {
    override fun invoke(viewModel: ViewModel) = when (viewModel) {
        is HtmlViewModel -> buildString { with(viewModel) { appendHTML().render() } }
        else -> throw ViewNotFound(viewModel)
    }
}
