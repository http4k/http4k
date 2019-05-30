package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.uri
import org.http4k.lens.webForm

sealed class AccessTokenRequest

data class AuthorizationCodeAccessTokenRequest(
    val grantType: String,
    val clientId: ClientId,
    val clientSecret: String,
    val redirectUri: Uri,
    val authorizationCode: AuthorizationCode
) : AccessTokenRequest()

object ClientCredentialsTokenRequest : AccessTokenRequest()

fun Request.accessTokenRequest(): Result<AccessTokenRequest, AccessTokenError> {
    grantTypeForm(this).let {
        val grantType = grantType(it)
        return when (grantType) {
            "authorization_code" -> Success(AuthorizationCodeAccessTokenForm.extract(this))
            "client_credentials" -> Success(ClientCredentialsTokenRequest)
            else -> Failure(UnsupportedGrantType(grantType))
        }
    }
}

private object AuthorizationCodeAccessTokenForm {
    private val authorizationCode = FormField.map(::AuthorizationCode, AuthorizationCode::value).required("code")
    private val redirectUri = FormField.uri().required("redirect_uri")

    private val clientSecret = FormField.required("client_secret")
    private val clientId = FormField.map(::ClientId, ClientId::value).required("client_id")

    val accessTokenForm = Body.webForm(Validator.Strict,
        authorizationCode, redirectUri, grantType, clientId, clientSecret
    ).toLens()

    fun extract(request: Request): AuthorizationCodeAccessTokenRequest =
        with(accessTokenForm(request)) {
            AuthorizationCodeAccessTokenRequest(
                grantType(this),
                clientId(this),
                clientSecret(this),
                redirectUri(this),
                authorizationCode(this))
        }
}

private val grantType = FormField.required("grant_type")
val grantTypeForm = Body.webForm(Validator.Strict, grantType).toLens()