package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF
import org.http4k.security.openid.IdTokenConsumer

/**
 * Provides a configured set of objects for use with an OAuth2 provider.
 */
class OAuthProvider(
        providerConfig: OAuthProviderConfig,
        client: HttpHandler,
        callbackUri: Uri,
        scopes: List<String>,
        oAuthPersistence: OAuthPersistence,
        modifyAuthState: (Uri) -> Uri = { it },
        generateCrsf: CsrfGenerator = SECURE_CSRF,
        responseType: ResponseType = ResponseType.Code,
        idTokenConsumer: IdTokenConsumer = IdTokenConsumer.NoOp
) {

    // pre-configured API client for this provider
    val api = ClientFilters.SetHostFrom(providerConfig.apiBase).then(client)

    // use this filter to protect endpoints
    val authFilter: Filter = OAuthRedirectionFilter(providerConfig, callbackUri, scopes, generateCrsf, modifyAuthState, oAuthPersistence, responseType)

    // this HttpHandler should exist at the callback URI registered with the OAuth Provider
    val callback: HttpHandler = OAuthCallback(providerConfig, api, callbackUri, oAuthPersistence, idTokenConsumer)

    companion object
}

