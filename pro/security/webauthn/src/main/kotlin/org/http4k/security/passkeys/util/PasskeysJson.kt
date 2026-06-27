/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.util

import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value

data class RegistrationRequest(val name: String, val displayName: String? = null)

data class AuthenticationResult(val userHandle: Base64UriBlob)

object PasskeysJson : ConfigurableMoshi(
    standardConfig()
        .value(Base64UriBlob)
        .done()
) {
    inline fun <reified T : Any> Request.json(): T = autoBody<T>().toLens()(this)
    inline fun <reified T : Any> Request.json(body: T): Request = with(autoBody<T>().toLens() of body)

    inline fun <reified T : Any> Response.json(): T = autoBody<T>().toLens()(this)
    inline fun <reified T : Any> Response.json(body: T): Response = with(autoBody<T>().toLens() of body)
}
