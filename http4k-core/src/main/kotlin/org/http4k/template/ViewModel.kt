package org.http4k.template

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.string

interface ViewModel {
    /**
     * This is the path of the template file - which matches the fully qualified classname. The templating suffix
     * is added by the template implementation (eg. java.lang.String -> java/lang/String.hbs)
     */
    fun template(): String = javaClass.name.replace('.', '/')
}

fun Body.Companion.viewModel(renderer: TemplateRenderer, contentType: ContentType) =
    string(contentType)
        .map<ViewModel>({ throw UnsupportedOperationException("Cannot parse a ViewModel") }, renderer::invoke)

@Deprecated("Use viewModel() instead", ReplaceWith("Body.viewModel(renderer, contentType).toLens()"))
fun Body.Companion.view(renderer: TemplateRenderer, contentType: ContentType): BiDiBodyLens<ViewModel> =
    viewModel(renderer, contentType).toLens()
