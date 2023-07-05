package org.http4k.template

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.string
import org.http4k.websocket.WsMessage

interface ViewModel {
    /**
     * This is the path of the template file - which matches the fully qualified classname. The templating suffix
     * is added by the template implementation (eg. java.lang.String -> java/lang/String.hbs)
     */
    fun template(): String = javaClass.name.replace('.', '/')

    /**
     * This is the model to use in the template, or `null` to not use any model.
     */
    fun model(): Any? = this
}

/**
 * Use a template with a specific model.
 * Does not work with Rocker.
 */
data class ViewWithModel(val template: String, val model: Any) : ViewModel {
    override fun template() = template.replace('.', '/')
    override fun model() = model
}

/**
 * Use a template with a specific model.
 * Does not work with Rocker.
 */
data class View(val template: String) : ViewModel {
    override fun template() = template.replace('.', '/')
    override fun model() = null
}

fun Body.Companion.viewModel(renderer: TemplateRenderer, contentType: ContentType) =
    string(contentType)
        .map<ViewModel>({ throw UnsupportedOperationException("Cannot parse a ViewModel") }, renderer::invoke)

fun WsMessage.Companion.viewModel(renderer: TemplateRenderer) =
    string().map<ViewModel>({ throw UnsupportedOperationException("Cannot parse a ViewModel") }, renderer::invoke)
