package org.http4k.contract.ui

import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.static

fun redocLite(configFn: RedocConfig.() -> Unit = {}): RoutingHttpHandler {
    return RedocConfig()
        .also(configFn)
        .toFilter("https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js")
        .then(static(ResourceLoader.Classpath("org/http4k/contract/ui/redoc")))
}
