package org.http4k.template

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLens

@Deprecated("Use viewModel() instead", ReplaceWith("Body.viewModel(renderer, contentType).toLens()"))
fun Body.Companion.view(renderer: TemplateRenderer, contentType: ContentType): BiDiBodyLens<ViewModel> =
    viewModel(renderer, contentType).toLens()
