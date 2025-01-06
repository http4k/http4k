package org.http4k.bridge

import io.vertx.ext.web.RoutingContext
import org.http4k.core.HttpHandler

/**
 * Bridge a single Vertx request to an Http4k handler.
 */
fun VertxToHttp4kHandler(http: HttpHandler): (RoutingContext) -> Unit = { ctx ->
    ctx.request().asHttp4k()
        .map(http)
        .map { it.into(ctx.response()) }
}
