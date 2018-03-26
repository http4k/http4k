package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.toUrlFormEncoded
import org.http4k.core.with
import org.http4k.lens.Header

internal class OAuthRedirectionFilter(
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val generateCrsf: CsrfGenerator = CrossSiteRequestForgeryToken.SECURE_CSRF,
    private val modifyState: (Uri) -> Uri,
    private val oAuthPersistence: OAuthPersistence
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = {
        if (oAuthPersistence.retrieveToken(it) != null) next(it) else {
            val csrf = generateCrsf()
            val redirect = Response(Status.TEMPORARY_REDIRECT).with(Header.Common.LOCATION of clientConfig.authUri
                .query("client_id", clientConfig.credentials.user)
                .query("response_type", "code")
                .query("scope", scopes.joinToString(" "))
                .query("redirect_uri", callbackUri.toString())
                .query("state", listOf("csrf" to csrf.value, "uri" to it.uri.toString()).toUrlFormEncoded())
                .with(modifyState))
            oAuthPersistence.withAssignedCsrf(redirect, csrf)
        }
    }
}