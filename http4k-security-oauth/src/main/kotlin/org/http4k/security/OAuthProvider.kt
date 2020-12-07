package org.http4k.security

import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.routing.bind
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.security.openid.Nonce.Companion.SECURE_NONCE
import org.http4k.security.openid.NonceGenerator
import org.http4k.security.openid.RequestJwts

/**
 * Provides a configured set of objects for use with an OAuth2 provider.
 */
class OAuthProvider(
    val providerConfig: OAuthProviderConfig,
    client: HttpHandler,
    private val callbackUri: Uri,
    val scopes: List<String>,
    private val oAuthPersistence: OAuthPersistence,
    private val modifyAuthState: (Uri) -> Uri = { it },
    private val generateCrsf: CsrfGenerator = SECURE_CSRF,
    private val nonceGenerator: NonceGenerator = SECURE_NONCE,
    private val responseType: ResponseType = ResponseType.Code,
    idTokenConsumer: IdTokenConsumer = IdTokenConsumer.NoOp,
    accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator = ClientSecretAccessTokenFetcherAuthenticator(providerConfig),
    standardRedirectionUrlBuilder: RedirectionUriBuilder = defaultUriBuilder,
    private val jwtRedirectionUriBuilder: (RequestJwts) -> RedirectionUriBuilder = ::uriBuilderWithRequestJwt
) {
    // pre-configured API client for this provider
    val api = ClientFilters.SetHostFrom(providerConfig.apiBase).then(client)

    // use this filter to protect endpoints
    val authFilter = OAuthRedirectionFilter(providerConfig, callbackUri, scopes, generateCrsf, nonceGenerator, modifyAuthState, oAuthPersistence, responseType, standardRedirectionUrlBuilder)

    // protect endpoint and provide custom request JWT creation mechanism
    fun authFilter(requestJwts: RequestJwts) =
        OAuthRedirectionFilter(providerConfig, callbackUri, scopes, generateCrsf, nonceGenerator, modifyAuthState, oAuthPersistence, responseType, jwtRedirectionUriBuilder(requestJwts))

    private val accessTokenFetcher = AccessTokenFetcher(api, callbackUri, providerConfig, accessTokenFetcherAuthenticator)

    // this HttpHandler should exist at the callback URI registered with the OAuth Provider
    val callback = OAuthCallback(oAuthPersistence, idTokenConsumer, accessTokenFetcher)

    // convenient binding of callback path to handler
    val callbackEndpoint = callbackUri.path bind callback

    companion object
}

