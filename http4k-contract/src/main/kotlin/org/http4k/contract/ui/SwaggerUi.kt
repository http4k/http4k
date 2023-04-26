package org.http4k.contract.ui

import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.routes
import org.http4k.routing.static

/**
 * Serve a "lite" Swagger UI, served by a public CDN
 */
fun swaggerUiLite(configFn: SwaggerUiConfig.() -> Unit = {}) = SwaggerUiConfig()
    .also(configFn)
    .toFilter()
    .then(routes(
        static(ResourceLoader.Classpath("org/http4k/contract/ui/swagger-config/")),
        static(ResourceLoader.Classpath("org/http4k/contract/ui/swagger"))
    ))
