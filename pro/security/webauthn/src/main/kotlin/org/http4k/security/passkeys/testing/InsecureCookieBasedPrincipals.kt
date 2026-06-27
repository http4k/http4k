/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.testing

import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.with
import org.http4k.lens.RequestLens
import org.http4k.security.passkeys.Principal.Anonymous
import org.http4k.security.passkeys.Principal.Known
import org.http4k.security.passkeys.Principals

class InsecureCookieBasedPrincipals(
    cookieNamePrefix: String,
    private val contextKey: RequestLens<Base64UriBlob>,
) : Principals {

    private val cookie = cookieNamePrefix + "_passkey_session"

    override fun read(request: Request) =
        fromCookie(request)?.let { Known(request.with(contextKey of it), it) } ?: Anonymous

    override fun write(userHandle: Base64UriBlob, response: Response) =
        response.cookie(Cookie(cookie, userHandle.value, path = "/"))

    override fun clear(response: Response): Response = response.invalidateCookie(cookie, path = "/")

    private fun fromCookie(request: Request) = request.cookie(cookie)?.value?.let(Base64UriBlob::of)
}
