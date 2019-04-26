package org.http4k.security.oauth.server

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.uri
import org.http4k.lens.webForm


data class AccessTokenRequest(
    val grantType: String,
    val clientId: ClientId,
    val clientSecret: String,
    val redirectUri: Uri,
    val authorizationCode: AuthorizationCode
)

fun Request.accessTokenRequest(): AccessTokenRequest = AccessTokenForm.extract(this)

private object AccessTokenForm {
    private val authorizationCode = FormField.map(::AuthorizationCode, AuthorizationCode::value).required("code")
    private val redirectUri = FormField.uri().required("redirect_uri")
    private val grantType = FormField.required("grant_type")
    private val clientSecret = FormField.required("client_secret")
    private val clientId = FormField.map(::ClientId, ClientId::value).required("client_id")

    val accessTokenForm = Body.webForm(Validator.Strict,
        authorizationCode, redirectUri, grantType, clientId, clientSecret
    ).toLens()

    fun extract(request: Request): AccessTokenRequest =
        with(accessTokenForm(request)) {
            AccessTokenRequest(
                grantType(this),
                clientId(this),
                clientSecret(this),
                redirectUri(this),
                authorizationCode(this))
        }
}
