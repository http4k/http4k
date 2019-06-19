package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.security.openid.RequestJwts

/**
 * Provides a configured set of objects for use with an OAuth2 provider.
 */
class OAuthProvider(
    val providerConfig: OAuthProviderConfig,
    client: HttpHandler,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val oAuthPersistence: OAuthPersistence,
    private val modifyAuthState: (Uri) -> Uri = { it },
    private val generateCrsf: CsrfGenerator = SECURE_CSRF,
    private val responseType: ResponseType = ResponseType.Code,
    idTokenConsumer: IdTokenConsumer = IdTokenConsumer.NoOp
) {

    // pre-configured API client for this provider
    val api = ClientFilters.SetHostFrom(providerConfig.apiBase).then(client)

    // use this filter to protect endpoints
    val authFilter: Filter = OAuthRedirectionFilter(providerConfig, callbackUri, scopes, generateCrsf, modifyAuthState, oAuthPersistence, responseType)

    // protect endpoint and provide custom request JWT creation mechanism
    fun authFilter(requestJwts: RequestJwts): Filter = OAuthRedirectionFilter(providerConfig, callbackUri, scopes, generateCrsf, modifyAuthState, oAuthPersistence, responseType, uriBuilderWithRequestJwt(requestJwts))

    private val accessTokenFetcher = AccessTokenFetcher(api, callbackUri, providerConfig)

    // this HttpHandler should exist at the callback URI registered with the OAuth Provider
    val callback: HttpHandler = OAuthCallback(oAuthPersistence, idTokenConsumer, accessTokenFetcher)

    companion object
}

