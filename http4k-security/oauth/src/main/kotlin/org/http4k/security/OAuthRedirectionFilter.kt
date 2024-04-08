package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.LOCATION
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF
import org.http4k.security.Nonce.Companion.SECURE_NONCE
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.ClientId

class OAuthRedirectionFilter(
    private val providerConfig: OAuthProviderConfig,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val generateCrsf: CsrfGenerator = SECURE_CSRF,
    private val nonceGenerator: NonceGenerator = SECURE_NONCE,
    private val modifyState: (Uri) -> Uri,
    private val oAuthPersistence: OAuthPersistence,
    private val responseType: ResponseType,
    private val redirectionBuilder: RedirectionUriBuilder = defaultUriBuilder,
    private val originalUri: (Request) -> Uri = Request::uri,
    private val responseMode: ResponseMode? = null
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        if (oAuthPersistence.retrieveToken(request) != null) next(request)
        else {
            val csrf = generateCrsf(request)
            val state = State(csrf.value)
            val nonce = if (responseType == CodeIdToken) nonceGenerator.invoke() else null
            val authRequest = AuthRequest(
                ClientId(providerConfig.credentials.user),
                scopes,
                callbackUri,
                state,
                responseType,
                nonce,
                responseMode
            )
            val redirectUri = modifyState(redirectionBuilder(providerConfig.authUri, authRequest, state, nonce))

            Response(TEMPORARY_REDIRECT).with(LOCATION of redirectUri)
                .let { response -> oAuthPersistence.assignCsrf(response, csrf) }
                .let { response -> oAuthPersistence.assignOriginalUri(response, originalUri(request)) }
                .let { response -> if (nonce != null) oAuthPersistence.assignNonce(response, nonce) else response }
        }
    }
}
