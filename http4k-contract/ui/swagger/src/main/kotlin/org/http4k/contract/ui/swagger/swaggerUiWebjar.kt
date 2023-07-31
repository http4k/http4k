package org.http4k.contract.ui.swagger

import org.http4k.contract.ui.SwaggerUiConfig
import org.http4k.contract.ui.toFilter
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.appendToPath
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static

/**
 * Serve locally hosted Swagger UI
 */
fun swaggerUiWebjar(configFn: SwaggerUiConfig.() -> Unit = {}) = routes(
    "" bind Method.GET to { Response(Status.FOUND).with(Header.LOCATION of it.uri.appendToPath("index.html")) },

    SwaggerUiConfig()
        .also(configFn)
        .toFilter()
        .then(static(ResourceLoader.Classpath("org/http4k/contract/ui/swagger-config"))),

    static(ResourceLoader.Classpath( "/META-INF/resources/webjars/swagger-ui/5.1.3"))
)
