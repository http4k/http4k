package org.http4k.security

import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.openid.RequestJwts

typealias RedirectionUriBuilder = (Uri, AuthRequest, state: State, nonce: Nonce?) -> Uri

val defaultUriBuilder: RedirectionUriBuilder = { uri: Uri, authRequest: AuthRequest, state: State, nonce: Nonce? ->
    val oauthUri = uri.query("client_id", authRequest.client.value)
        .query("response_type", authRequest.responseType.queryParameterValue)
        .query("scope", authRequest.scopes.joinToString(" "))
        .query("redirect_uri", authRequest.redirectUri.toString())
        .query("state", state.value)
    if (nonce != null) {
        oauthUri.query("nonce", nonce.value)
    } else {
        oauthUri
    }
}

fun uriBuilderWithRequestJwt(requestJwts: RequestJwts) = { uri: Uri, authRequest: AuthRequest, state: State, nonce: Nonce? ->
    defaultUriBuilder(uri, authRequest, state, nonce)
        .query("request", requestJwts.create(authRequest, state, nonce).value)
}
