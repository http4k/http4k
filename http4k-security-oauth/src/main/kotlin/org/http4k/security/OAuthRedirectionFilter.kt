package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded
import org.http4k.core.with
import org.http4k.lens.Header.LOCATION
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.ClientId

class OAuthRedirectionFilter(
    private val providerConfig: OAuthProviderConfig,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val generateCrsf: CsrfGenerator = CrossSiteRequestForgeryToken.SECURE_CSRF,
    private val modifyState: (Uri) -> Uri,
    private val oAuthPersistence: OAuthPersistence,
    private val responseType: ResponseType,
    private val redirectionBuilder: RedirectionUriBuilder = defaultUriBuilder
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = {
        if (oAuthPersistence.retrieveToken(it) != null) next(it) else {
            val csrf = generateCrsf()
            val state = listOf("csrf" to csrf.value, "uri" to it.uri.toString()).toUrlFormEncoded()

            val authRequest = AuthRequest(
                ClientId(providerConfig.credentials.user),
                scopes,
                callbackUri,
                state,
                responseType
            )

            val redirect = Response(TEMPORARY_REDIRECT)
                .with(
                    LOCATION of redirectionBuilder(providerConfig.authUri, authRequest, state)
                        .with(modifyState)
                )

            oAuthPersistence.assignCsrf(redirect, csrf)
        }
    }
}