package org.http4k.template

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE

typealias TemplateRenderer = (ViewModel) -> String

/**
 * Supported template implementations for templating engine implementations
 */
interface Templates {

    /**
     * Loads and caches templates from the compiled classpath
     *
     * @param baseClasspathPackage the root package to load from (defaults to root)
     */
    fun CachingClasspath(baseClasspathPackage: String = ""): TemplateRenderer

    /**
     * Load and caches templates from a file path
     *
     * @param baseTemplateDir the root path to load templates from
     */
    fun Caching(baseTemplateDir: String = "./"): TemplateRenderer

    /**
     * Hot-reloads (no-caching) templates from a file path
     *
     * @param baseTemplateDir the root path to load templates from
     */
    fun HotReload(baseTemplateDir: String = "./"): TemplateRenderer
}

/**
 * Compose a TemplateRenderer with another, so you can fall back.
 */
fun TemplateRenderer.then(that: TemplateRenderer): TemplateRenderer = {
    try {
        this(it)
    } catch (e: ViewNotFound) {
        that(it)
    }
}

/**
 * Convenience method for generating a Response from a view model.
 */
fun TemplateRenderer.renderToResponse(
    viewModel: ViewModel,
    status: Status = OK,
    contentType: ContentType = TEXT_HTML
): Response =
    Response(status).with(CONTENT_TYPE of contentType).body(invoke(viewModel))
