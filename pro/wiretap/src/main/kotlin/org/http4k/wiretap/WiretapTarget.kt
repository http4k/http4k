/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.filter.ClientFilters

fun interface WiretapTarget {
    operator fun invoke(setup: WiretapContext): Wiretapped
}


class RemoteTarget(private val fn: WiretapContext.() -> Uri) : WiretapTarget {
    constructor(uri: Uri) : this({ uri })

    override operator fun invoke(setup: WiretapContext): Wiretapped {
        val uri = setup.fn()
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

class LocalTarget(private val fn: WiretapContext.() -> HttpHandler) : WiretapTarget {
    override operator fun invoke(setup: WiretapContext): Wiretapped {
        val app = setup.fn()
        return object : Wiretapped {
            override fun supportsMcp(http: HttpHandler, mcpPath: String) =
                app(Request(GET, Uri.of(mcpPath))).status.successful

            override fun using(httpClient: HttpHandler) = app
        }
    }
}

interface Wiretapped {
    fun supportsMcp(http: HttpHandler, mcpPath: String): Boolean
    fun using(httpClient: HttpHandler): HttpHandler
}
