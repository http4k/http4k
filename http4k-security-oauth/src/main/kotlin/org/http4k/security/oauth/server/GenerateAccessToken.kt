package org.http4k.security.oauth.server

import org.http4k.core.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.*
import org.http4k.security.AccessTokenResponse
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.server.GenerateAccessToken.Companion.authorizationCode
import org.http4k.security.oauth.server.GenerateAccessToken.Companion.clientId
import org.http4k.security.oauth.server.GenerateAccessToken.Companion.clientSecret
import org.http4k.security.oauth.server.GenerateAccessToken.Companion.redirectUri
import java.time.Clock

class GenerateAccessToken(
    private val clientValidator: ClientValidator,
    private val authorizationCodes: AuthorizationCodes,
    private val accessTokens: AccessTokens,
    private val clock: Clock,
    private val idTokens: IdTokens,
    private val json : AutoMarshallingJson,
    private val documentationUri : String = ""
) : HttpHandler {

    override fun invoke(request: Request): Response {
        val form = accessTokenForm(request)
        val accessTokenRequest = form.accessTokenRequest()

        if (grantType(form) != "authorization_code") {
            return Response(BAD_REQUEST).body(errorResponse("unsupported_grant_type", "${grantType(form)} is not supported"))
        }

        if (!clientValidator.validateCredentials(accessTokenRequest.clientId, accessTokenRequest.clientSecret)) {
            return Response(UNAUTHORIZED).body(errorResponse("invalid_client", "Client id and secret unrecognised"))
        }

        val code = accessTokenRequest.authorizationCode
        val codeDetails = authorizationCodes.detailsFor(code)

        if (codeDetails.expiresAt.isBefore(clock.instant())) {
            return Response(BAD_REQUEST).body(errorResponse("invalid_grant", "The authorization code has expired"))
        }

        if (codeDetails.clientId != accessTokenRequest.clientId) {
            return Response(BAD_REQUEST).body(errorResponse("invalid_grant", "The 'client_id' parameter does not match the authorization request"))
        }

        if (codeDetails.redirectUri != accessTokenRequest.redirectUri) {
            return Response(BAD_REQUEST).body(errorResponse("invalid_grant", "The 'redirect_uri' parameter does not match the authorization request"))
        }

        val accessToken = accessTokens.create(code)

        return Response(OK).let {
            when (codeDetails.responseType) {
                Code -> it.body(accessToken.value)
                CodeIdToken -> {
                    val idToken = idTokens.createForAccessToken(code)
                    it.with(accessTokenResponseBody of AccessTokenResponse(accessToken.value, idToken.value))
                }
            }
        }.also { authorizationCodes.destroy(code) }
    }

    private fun errorResponse(errorCode: String, errorDescription: String) = json.asJsonString(ErrorResponse(errorCode, errorDescription, documentationUri))

    companion object {
        internal val authorizationCode = FormField.map(::AuthorizationCode, AuthorizationCode::value).required("code")
        internal val redirectUri = FormField.uri().required("redirect_uri")
        private val grantType = FormField.required("grant_type")
        internal val clientSecret = FormField.required("client_secret")
        internal val clientId = FormField.map(::ClientId, ClientId::value).required("client_id")

        val accessTokenForm = Body.webForm(Validator.Strict,
            authorizationCode, redirectUri, grantType, clientId, clientSecret
        ).toLens()
    }
}

data class ErrorResponse(val error: String, val error_description: String, val error_uri: String)

private fun WebForm.accessTokenRequest() =
    AccessTokenRequest(
        clientId(this),
        clientSecret(this),
        redirectUri(this),
        authorizationCode(this)
    )

data class AccessTokenRequest(
    val clientId: ClientId,
    val clientSecret: String,
    val redirectUri: Uri,
    val authorizationCode: AuthorizationCode)