package org.http4k.security

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.long
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenFetcher.Companion.Forms.clientId
import org.http4k.security.AccessTokenFetcher.Companion.Forms.code
import org.http4k.security.AccessTokenFetcher.Companion.Forms.grantType
import org.http4k.security.AccessTokenFetcher.Companion.Forms.redirectUri
import org.http4k.security.AccessTokenFetcher.Companion.Forms.requestForm
import org.http4k.security.AccessTokenFetcher.Companion.Forms.responseForm
import org.http4k.security.oauth.server.refreshtoken.RefreshToken
import org.http4k.security.openid.IdToken

class AccessTokenFetcher(
    private val api: HttpHandler,
    private val callbackUri: Uri,
    private val providerConfig: OAuthProviderConfig,
    private val accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator
) {
    fun fetch(theCode: String): AccessTokenDetails? = api(
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
    )
        .let { if (it.status != OK) null else it }
        ?.let { msg ->
            CONTENT_TYPE(msg)
                ?.let {
                    when {
                        APPLICATION_JSON.equalsIgnoringDirectives(it) -> with(accessTokenResponseBody(msg)) {
                            AccessTokenDetails(toAccessToken(), idToken?.let(::IdToken))
                        }
                        APPLICATION_FORM_URLENCODED.equalsIgnoringDirectives(it) -> responseForm(msg)
                        else -> null
                    }
                } ?: AccessTokenDetails(AccessToken(msg.bodyString()))
        }

    private fun Request.authenticate(accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator): Request =
        accessTokenFetcherAuthenticator.authenticate(this)

    private fun AccessTokenResponse.toAccessToken(): AccessToken =
        AccessToken(
            accessToken,
            type = tokenType ?: "Bearer",
            expiresIn = expiresIn,
            scope = scope,
            refreshToken = refreshToken?.let(::RefreshToken)
        )

    companion object {
        object Forms {
            // response
            val accessToken = FormField.string().required("access_token")
            val tokenType = FormField.string().defaulted("token_type", "Bearer")
            val expiresIn = FormField.long().optional("expires_in")
            val idToken = FormField.string().map(::IdToken, IdToken::value).optional("id_token")
            val scope = FormField.string().optional("scope")
            val refreshToken = FormField.string().map(::RefreshToken, RefreshToken::value).optional("refresh_token")

            val responseForm =
                Body.webForm(Validator.Strict, accessToken, tokenType, expiresIn, idToken, scope, refreshToken)
                    .map({
                        AccessTokenDetails(
                            AccessToken(
                                accessToken(it),
                                tokenType(it),
                                expiresIn(it),
                                scope(it),
                                refreshToken(it)
                            ), idToken(it)
                        )
                    },
                        {
                            WebForm().with(
                                accessToken of it.accessToken.value,
                                tokenType of (it.accessToken.type ?: "Bearer"),
                                expiresIn of it.accessToken.expiresIn,
                                scope of it.accessToken.scope,
                                refreshToken of it.accessToken.refreshToken,
                                idToken of it.idToken
                            )
                        }
                    ).toLens()

            // request
            val grantType = FormField.string().required("grant_type")
            val clientId = FormField.string().required("client_id")
            val redirectUri = FormField.uri().required("redirect_uri")
            val code = FormField.string().required("code")

            val requestForm = Body.webForm(Validator.Strict, grantType, clientId, redirectUri, code).toLens()
        }
    }
}
