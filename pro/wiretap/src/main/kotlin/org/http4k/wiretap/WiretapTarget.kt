/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.filter.ClientFilters

fun interface WiretapTarget {
    operator fun invoke(http: HttpHandler, oTel: OpenTelemetry): Wiretapped
}


class RemoteTarget(private val fn: (HttpHandler, OpenTelemetry) -> Uri) : WiretapTarget {
    constructor(uri: Uri) : this({ _, _ -> uri })

    override operator fun invoke(http: HttpHandler, oTel: OpenTelemetry): Wiretapped {
        val uri = fn(http, oTel)
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

class LocalTarget(private val fn: (HttpHandler, OpenTelemetry) -> HttpHandler) : WiretapTarget {
    constructor(http: HttpHandler) : this({ _, _ -> http })

    override operator fun invoke(http: HttpHandler, oTel: OpenTelemetry): Wiretapped {
        val app = fn(http, oTel)
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
