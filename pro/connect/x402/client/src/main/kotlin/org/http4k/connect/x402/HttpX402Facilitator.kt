/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.x402

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

fun X402Facilitator.Companion.Http(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(),
) = object : X402Facilitator {
    private val routedHttp = ClientFilters.SetBaseUriFrom(baseUri).then(http)

    override fun <R> invoke(action: X402FacilitatorAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
