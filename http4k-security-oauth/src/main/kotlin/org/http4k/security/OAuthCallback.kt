package org.http4k.security

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.toParameters
import org.http4k.core.with
import org.http4k.lens.Header

internal class OAuthCallback(
    private val api: HttpHandler,
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val oAuthPersistence: OAuthPersistence
) : HttpHandler {

    private fun codeToAccessToken(code: String) =
        api(Request(Method.POST, clientConfig.tokenPath)
            .with(Header.Common.CONTENT_TYPE of ContentType.APPLICATION_FORM_URLENCODED)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", callbackUri.toString())
            .form("client_id", clientConfig.credentials.user)
            .form("client_secret", clientConfig.credentials.password)
            .form("code", code))
            .let { if (it.status == Status.OK) AccessToken(it.bodyString()) else null }

    override fun invoke(request: Request): Response {
        val state = request.query("state")?.toParameters() ?: emptyList()
        val crsfInState = state.find { it.first == "csrf" }?.second?.let(::CrossSiteRequestForgeryToken)
        return request.query("code")?.let { code ->
            if (crsfInState != null && crsfInState == oAuthPersistence.retrieveCsrf(request)) {
                codeToAccessToken(code)?.let {
                    val originalUri = state.find { it.first == "uri" }?.second ?: "/"
                    oAuthPersistence.withAssignedToken(Response(Status.TEMPORARY_REDIRECT).header("Location", originalUri), it)
                }
            } else null
        } ?: oAuthPersistence.authFailureResponse()
    }
}