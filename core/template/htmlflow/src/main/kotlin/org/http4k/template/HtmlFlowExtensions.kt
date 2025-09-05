package org.http4k.template

import htmlflow.HtmlView

/**
 * Creates a [TemplateRenderer] from an [HtmlView].
 *
 * The resulting renderer will render the provided view model using the [HtmlView.render] method.
 *
 * @throws [ViewNotFound] if the provided view model is not of the expected type [T].
 */
fun <T : ViewModel> HtmlView<T>.renderer(): TemplateRenderer {
    return { viewModel: ViewModel ->
        try {
            @Suppress("UNCHECKED_CAST") this.render(viewModel as T)
        } catch (_: ClassCastException) {
            throw ViewNotFound(viewModel)
        }
    }
}
