/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.authentication

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.fanbelt.http.Guard
import org.http4k.lens.RequestLens

fun <Principal : Any> Authenticated(
    principal: RequestLens<Principal>,
    refused: HttpHandler = { Response(UNAUTHORIZED) },
    authenticate: (Request) -> Principal?,
) = Guard(UNAUTHORIZED, refused) { request -> authenticate(request)?.let { request.with(principal of it) } }
