package org.http4k.contract.ui

import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.routes
import org.http4k.routing.static

@Deprecated("use swaggerUiLite")
fun swaggerUi(
    descriptionRoute: Uri,
    title: String = "Swagger UI",
    displayOperationId: Boolean = false,
    displayRequestDuration: Boolean = false,
    requestSnippetsEnabled: Boolean = false,
    persistAuthorization: Boolean = false,
    queryConfigEnabled: Boolean = false,
    tryItOutEnabled: Boolean = false,
) = swaggerUiLite {
    this.pageTitle = title
    this.url = descriptionRoute.toString()
    this.displayOperationId = displayOperationId
    this.displayRequestDuration = displayRequestDuration
    this.requestSnippetsEnabled = requestSnippetsEnabled
    this.persistAuthorization = persistAuthorization
    this.queryConfigEnabled = queryConfigEnabled
    this.tryItOutEnabled = tryItOutEnabled
    this.deepLinking = true
    this.layout = "StandaloneLayout"
    this.presets += "SwaggerUIStandalonePreset"
}

/**
 * Serve a "lite" Swagger UI, served by a public CDN
 */
fun swaggerUiLite(configFn: SwaggerUiConfig.() -> Unit = {}) = SwaggerUiConfig()
    .also(configFn)
    .toFilter()
    .then(routes(
        static(ResourceLoader.Classpath("org/http4k/contract/ui/public-config/")),
        static(ResourceLoader.Classpath("org/http4k/contract/ui/public"))
    ))
