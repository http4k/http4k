package org.http4k.bridge

import io.vertx.ext.web.RoutingContext
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED

/**
 * Bridge a single Vertx request to an Http4k handler.
 */
fun VertxToHttp4kHandler(http: HttpHandler): (RoutingContext) -> Unit = { ctx ->
    val request = ctx.request().asHttp4k(ctx.vertx())
    ctx.vertx().executeBlocking({
        (request?.let(http) ?: Response(NOT_IMPLEMENTED)).into(ctx.response())
    }, false)
}
