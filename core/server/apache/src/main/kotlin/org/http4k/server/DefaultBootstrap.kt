package org.http4k.server

import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap
import org.apache.hc.core5.http.impl.routing.RequestRouter
import org.apache.hc.core5.http.io.HttpRequestHandler
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.net.URIAuthority
import org.http4k.core.HttpHandler

fun defaultBootstrap(port: Int, http: HttpHandler, canonicalHostname: String): ServerBootstrap {
    val fallbackAuthority = URIAuthority.create("fallback")

    return ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setCanonicalHostName(canonicalHostname)
        .setSocketConfig(
            SocketConfig.custom()
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)
                .setSoReuseAddress(true)
                .setBacklogSize(1000)
                .build()
        )
        .setRequestRouter(
            RequestRouter.builder<HttpRequestHandler>()
                .addRoute(fallbackAuthority, "*", Http4kRequestHandler(http))
                .resolveAuthority { _: String, _: URIAuthority -> fallbackAuthority }
                .build())
}
