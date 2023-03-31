package org.http4k.contract.ui.redoc

import org.http4k.contract.ui.RedocConfig
import org.http4k.contract.ui.toFilter
import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.routes
import org.http4k.routing.static

fun redocWebjar(configFn: RedocConfig.() -> Unit = {}) = RedocConfig()
    .also(configFn)
    .toFilter("redoc.standalone.js")
    .then(routes(
        static(ResourceLoader.Classpath("org/http4k/contract/ui/redoc/")),
        static(ResourceLoader.Classpath( "/META-INF/resources/webjars/redoc/2.0.0"))
    ))
