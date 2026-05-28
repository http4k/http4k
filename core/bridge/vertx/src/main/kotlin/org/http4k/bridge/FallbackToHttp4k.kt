package org.http4k.bridge

import io.vertx.ext.web.Router
import org.http4k.core.HttpHandler

/**
 * Fallback to Http4k handler as a wildcard route.
 */
fun Router.fallbackToHttp4k(function: HttpHandler) {
    route("/*").handler(VertxToHttp4kHandler(function))
}
