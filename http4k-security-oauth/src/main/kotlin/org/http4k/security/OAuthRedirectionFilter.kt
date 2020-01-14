package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded
import org.http4k.core.with
import org.http4k.lens.Header.LOCATION
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.openid.Nonce
import org.http4k.security.openid.Nonce.Companion.SECURE_NONCE
import org.http4k.security.openid.NonceGenerator

class OAuthRedirectionFilter(
    private val providerConfig: OAuthProviderConfig,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val generateCrsf: CsrfGenerator = SECURE_CSRF,
    private val nonceGenerator: NonceGenerator = SECURE_NONCE,
    private val modifyState: (Uri) -> Uri,
    private val oAuthPersistence: OAuthPersistence,
    private val responseType: ResponseType,
    private val redirectionBuilder: RedirectionUriBuilder = defaultUriBuilder
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = {
        if (oAuthPersistence.retrieveToken(it) != null) next(it) else {
            val csrf = generateCrsf()
            val state = listOf("csrf" to csrf.value, "uri" to it.uri.toString()).toUrlFormEncoded()
            val nonce = generateNonceIfRequired()

            val authRequest = AuthRequest(
                ClientId(providerConfig.credentials.user),
                scopes,
                callbackUri,
                state,
                responseType,
                nonce
            )

            val redirect = Response(TEMPORARY_REDIRECT)
                .with(
                    LOCATION of redirectionBuilder(providerConfig.authUri, authRequest, state)
                        .with(modifyState)
                )

            assignNonceIfRequired(oAuthPersistence.assignCsrf(redirect, csrf), nonce)
        }
    }

    private fun generateNonceIfRequired() = if (responseType == ResponseType.CodeIdToken) nonceGenerator.invoke() else null

    private fun assignNonceIfRequired(redirect: Response, nonce: Nonce?): Response =
        if (nonce != null) {
            oAuthPersistence.assignNonce(redirect, nonce)
        } else {
            redirect
        }
}
