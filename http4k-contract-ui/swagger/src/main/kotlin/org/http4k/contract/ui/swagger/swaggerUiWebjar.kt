package org.http4k.contract.ui.swagger

import org.http4k.contract.ui.SwaggerUiConfig
import org.http4k.contract.ui.toFilter
import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.routes
import org.http4k.routing.static

fun swaggerUiWebjar(configFn: SwaggerUiConfig.() -> Unit = {}) = SwaggerUiConfig()
    .also(configFn)
    .toFilter()
    .then(routes(
        static(ResourceLoader.Classpath("org/http4k/contract/ui/public-config/")),
        static(ResourceLoader.Classpath( "/META-INF/resources/webjars/swagger-ui/4.18.1"))
    ))
