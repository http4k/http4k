package org.http4k.security

import org.http4k.core.*
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.lens.Header.LOCATION
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken

class OAuthRedirectionFilter(
        private val providerConfig: OAuthProviderConfig,
        private val callbackUri: Uri,
        private val scopes: List<String>,
        private val generateCrsf: CsrfGenerator = CrossSiteRequestForgeryToken.SECURE_CSRF,
        private val modifyState: (Uri) -> Uri,
        private val oAuthPersistence: OAuthPersistence,
        private val responseType: ResponseType
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = {
        if (oAuthPersistence.retrieveToken(it) != null) next(it) else {
            val csrf = generateCrsf()
            val redirect = Response(TEMPORARY_REDIRECT).with(LOCATION of providerConfig.authUri
                .query("client_id", providerConfig.credentials.user)
                .query("response_type", responseType.toRequestParameter())
                .query("scope", scopes.joinToString(" "))
                .query("redirect_uri", callbackUri.toString())
                .query("state", listOf("csrf" to csrf.value, "uri" to it.uri.toString()).toUrlFormEncoded())
                .with(modifyState))
            oAuthPersistence.assignCsrf(redirect, csrf)
        }
    }

    private fun ResponseType.toRequestParameter(): String = when (this) {
        Code -> "code"
        CodeIdToken -> "code id_token"
    }
}