package org.http4k.template

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.string
import org.http4k.websocket.WsMessage

interface ViewModel {
    /**
     * This is the path of the template file - which matches the fully qualified classname. The templating suffix
     * is added by the template implementation (eg. java.lang.String -> java/lang/String.hbs)
     *
     * SECURITY: file-backed loaders (`Caching`/`HotReload`) pass this value straight to the engine's file resolver.
     * Overriding this with request-derived input enables path traversal (`../../etc/passwd`); keep it developer-controlled.
     */
    fun template(): String = javaClass.name.replace('.', '/')
}

fun Body.Companion.viewModel(renderer: TemplateRenderer, contentType: ContentType) =
    string(contentType)
        .map<ViewModel>({ throw UnsupportedOperationException("Cannot parse a ViewModel") }, renderer::invoke)

fun WsMessage.Companion.viewModel(renderer: TemplateRenderer) =
    string().map<ViewModel>({ throw UnsupportedOperationException("Cannot parse a ViewModel") }, renderer::invoke)
