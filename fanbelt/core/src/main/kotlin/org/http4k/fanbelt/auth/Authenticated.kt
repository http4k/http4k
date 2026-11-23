/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.auth

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.fanbelt.http.Guard
import org.http4k.lens.RequestLens

/**
 * A Guard that authenticates each request and puts the principal in a RequestLens. Unauthenticated requests get a 401
 * unless onRefused says otherwise.
 */
fun <Principal : Any> Authenticated(
    principal: RequestLens<Principal>,
    onRefused: HttpHandler = { Response(UNAUTHORIZED) },
    authenticate: (Request) -> Principal?,
) = Guard(onRefused) { request -> authenticate(request)?.let { request.with(principal of it) } }
