package org.http4k.security

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.WebForm
import org.http4k.security.OAuthCallbackError.CouldNotFetchAccessToken
import org.http4k.security.OAuthWebForms.clientId
import org.http4k.security.OAuthWebForms.code
import org.http4k.security.OAuthWebForms.grantType
import org.http4k.security.OAuthWebForms.redirectUri
import org.http4k.security.OAuthWebForms.requestForm

class AccessTokenFetcher(
    private val api: HttpHandler,
    private val callbackUri: Uri,
    private val providerConfig: OAuthProviderConfig,
    private val accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator,
    private val accessTokenExtractor: AccessTokenExtractor = ContentTypeJsonOrForm()
) {
    fun fetch(theCode: String): Result<AccessTokenDetails, CouldNotFetchAccessToken> =
        api(
            Request(POST, providerConfig.tokenPath)
                .with(
                    requestForm of WebForm()
                        .with(
                            grantType of "authorization_code",
                            redirectUri of callbackUri,
                            clientId of providerConfig.credentials.user,
                            code of theCode
                        )
                )
                .authenticate(accessTokenFetcherAuthenticator)
        ).let {
            if (it.status != OK) Failure(CouldNotFetchAccessToken(it.status, it.bodyString()))
            else Success(it)
        }.flatMap { msg -> accessTokenExtractor(msg) }

    private fun Request.authenticate(accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator): Request =
        accessTokenFetcherAuthenticator.authenticate(this)
}
