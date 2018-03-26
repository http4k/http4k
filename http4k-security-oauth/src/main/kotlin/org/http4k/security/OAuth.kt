package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF

class OAuth(client: HttpHandler,
            clientConfig: OAuthConfig,
            callbackUri: Uri,
            scopes: List<String>,
            oAuthPersistence: OAuthPersistence,
            modifyAuthState: (Uri) -> Uri = { it },
            generateCrsf: CsrfGenerator = SECURE_CSRF) {

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter: Filter = OAuthRedirectionFilter(clientConfig, callbackUri, scopes, generateCrsf, modifyAuthState, oAuthPersistence)

    val callback: HttpHandler = OAuthCallback(api, clientConfig, callbackUri, oAuthPersistence)

    companion object
}

