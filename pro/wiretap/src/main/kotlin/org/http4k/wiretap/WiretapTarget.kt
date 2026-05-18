/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.testing.toHttpHandler

fun interface WiretapTarget {
    operator fun invoke(ctx: Context): Wiretapped
}

/**
 * A WiretapTarget that points to a running server.
 */
class RemoteTarget(private val fn: Context.() -> Uri) : WiretapTarget {
    constructor(uri: Uri) : this({ uri })

    override operator fun invoke(ctx: Context): Wiretapped {
        val uri = ctx.fn()
        return object : Wiretapped {
            override fun supportsMcp(http: HttpHandler, mcpPath: String) =
                http(Request(GET, uri.extend(Uri.of(mcpPath)))).status.successful

            override fun using(httpClient: HttpHandler) =
                ClientFilters.SetBaseUriFrom(uri)
                    .then(ClientFilters.FollowRedirects())
                    .then(httpClient)
        }
    }
}

/**
 * A WiretapTarget that points to an in-memory server.
 */
class LocalTarget(private val fn: Context.() -> HttpHandler) : WiretapTarget {
    override operator fun invoke(ctx: Context): Wiretapped {
        val app = ctx.fn()
        return object : Wiretapped {
            override fun supportsMcp(http: HttpHandler, mcpPath: String) =
                app(Request(GET, Uri.of(mcpPath))).status.successful

            override fun using(httpClient: HttpHandler) = app
        }
    }

    companion object {
        /**
         * Wiretap an HttpHandler.
         */
        fun http(fn: Context.() -> HttpHandler): LocalTarget = LocalTarget(fn)
        /**
         * Wiretap a PolyHandler.
         */
        fun poly(fn: Context.() -> PolyHandler): LocalTarget = LocalTarget({ fn().toHttpHandler() })
    }
}

interface Wiretapped {
    fun supportsMcp(http: HttpHandler, mcpPath: String): Boolean
    fun using(httpClient: HttpHandler): HttpHandler
}
