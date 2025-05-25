package org.http4k.bridge

import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.runBlocking
import org.http4k.core.HttpHandler

/**
 * Bridge a single Vertx request to an Http4k handler.
 */
fun VertxToHttp4kHandler(http: HttpHandler): (RoutingContext) -> Unit = { ctx ->
    ctx.request().asHttp4k()
        .map {
            runBlocking { http(it) } // FIXME coroutine blocking
        }
        .map { it.into(ctx.response()) }
}
