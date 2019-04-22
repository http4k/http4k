package org.http4k.security

import org.http4k.core.*
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.body.form
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.security.openid.IdTokenContainer

class OAuthCallback(
        private val providerConfig: OAuthProviderConfig,
        private val api: HttpHandler,
        private val callbackUri: Uri,
        private val oAuthPersistence: OAuthPersistence,
        private val idTokenConsumer: IdTokenConsumer
) : HttpHandler {

    private fun codeToAccessToken(code: String) =
        api(Request(POST, providerConfig.tokenPath)
            .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", callbackUri.toString())
            .form("client_id", providerConfig.credentials.user)
            .form("client_secret", providerConfig.credentials.password)
            .form("code", code))
            .let { if (it.status == Status.OK) AccessTokenContainer(it.bodyString()) else null }

    override fun invoke(request: Request): Response {
        val state = request.query("state")?.toParameters() ?: emptyList()
        val crsfInState = state.find { it.first == "csrf" }?.second?.let(::CrossSiteRequestForgeryToken)
        return request.query("code")?.let { code ->
            if (crsfInState != null && crsfInState == oAuthPersistence.retrieveCsrf(request)) {
                request.query("id_token")?.let { idTokenConsumer.consume(IdTokenContainer(it)) }
                codeToAccessToken(code)?.let {
                    val originalUri = state.find { it.first == "uri" }?.second ?: "/"
                    oAuthPersistence.assignToken(request, Response(TEMPORARY_REDIRECT).header("Location", originalUri), it)
                }
            } else null
        } ?: oAuthPersistence.authFailureResponse()
    }
}