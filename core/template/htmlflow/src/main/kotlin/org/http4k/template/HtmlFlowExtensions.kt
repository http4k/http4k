package org.http4k.template

import htmlflow.HtmlView

fun <T : ViewModel> HtmlView<T>.renderer(): TemplateRenderer {
    return { viewModel: ViewModel ->
        try {
            @Suppress("UNCHECKED_CAST") this.render(viewModel as T)
        } catch (_: ClassCastException) {
            throw ViewNotFound(viewModel)
        }
    }
}
