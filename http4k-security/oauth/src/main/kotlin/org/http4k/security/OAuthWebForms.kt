package org.http4k.security

import org.http4k.core.Body
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.long
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.security.oauth.core.RefreshToken
import org.http4k.security.openid.IdToken

object OAuthWebForms  {
    val grantType = FormField.required("grant_type")
    val clientId = FormField.optional("client_id")
    val clientSecret = FormField.optional("client_secret")
    val refreshToken = FormField.map(::RefreshToken, RefreshToken::value).optional("refresh_token")
    val scope = FormField.optional("scope")
    val code = FormField.optional("code")
    val redirectUri = FormField.uri().optional("redirect_uri")
    val accessToken = FormField.optional("access_token")
    val username = FormField.optional("username")
    val password = FormField.optional("password")

    val requestForm = Body.webForm(
        Validator.Strict,
        grantType,
        clientId,
        clientSecret,
        refreshToken,
        scope,
        code,
        redirectUri,
        accessToken,
        username,
        password
    ).toLens()

    val tokenType = FormField.defaulted("token_type", "Bearer")
    val expiresIn = FormField.long().optional("expires_in")
    val idToken = FormField.map(::IdToken, IdToken::value).optional("id_token")
    val accessTokenResp = FormField.required("access_token")

    val responseForm =
        Body.webForm(Validator.Strict, accessTokenResp, tokenType, expiresIn, idToken, scope, refreshToken)
            .map({
                AccessTokenDetails(
                    AccessToken(
                        accessTokenResp(it),
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
}
