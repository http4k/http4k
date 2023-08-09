package org.http4k.security

import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.openid.RequestJwts

typealias RedirectionUriBuilder = (Uri, AuthRequest, state: State, nonce: Nonce?) -> Uri

val defaultUriBuilder: RedirectionUriBuilder = { uri: Uri, authRequest: AuthRequest, state: State, nonce: Nonce? ->
    uri.query("client_id", authRequest.client.value)
        .query("response_type", authRequest.responseType.queryParameterValue)
        .query("scope", authRequest.scopes.joinToString(" "))
        .query("redirect_uri", authRequest.redirectUri.toString())
        .query("state", state.value)
        .addQueryIfNotNull("nonce", nonce?.value)
        .addQueryIfNotNull("response_mode", authRequest.responseMode?.queryParameterValue)
}

private fun Uri.addQueryIfNotNull(name: String, value: String?) = when {
    value != null -> query(name, value)
    else -> this
}

fun uriBuilderWithRequestJwt(requestJwts: RequestJwts) =
    { uri: Uri, authRequest: AuthRequest, state: State, nonce: Nonce? ->
        defaultUriBuilder(uri, authRequest, state, nonce)
            .query("request", requestJwts.create(authRequest, state, nonce).value)
    }
