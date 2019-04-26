package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.security.AccessTokenResponse
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.accessTokenResponseBody
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
        val accessTokenRequest = request.accessTokenRequest()

        if (accessTokenRequest.grantType != "authorization_code") {
            return Response(BAD_REQUEST).body(errorResponse(Error.unsupported_grant_type, "${accessTokenRequest.grantType} is not supported"))
        }

        if (!clientValidator.validateCredentials(accessTokenRequest.clientId, accessTokenRequest.clientSecret)) {
            return Response(UNAUTHORIZED).body(errorResponse(Error.invalid_client, "Client id and secret unrecognised"))
        }

        val code = accessTokenRequest.authorizationCode
        val codeDetails = authorizationCodes.detailsFor(code)

        if (codeDetails.expiresAt.isBefore(clock.instant())) {
            return Response(BAD_REQUEST).body(errorResponse(Error.invalid_grant, "The authorization code has expired"))
        }

        if (codeDetails.clientId != accessTokenRequest.clientId) {
            return Response(BAD_REQUEST).body(errorResponse(Error.invalid_grant, "The 'client_id' parameter does not match the authorization request"))
        }

        if (codeDetails.redirectUri != accessTokenRequest.redirectUri) {
            return Response(BAD_REQUEST).body(errorResponse(Error.invalid_grant, "The 'redirect_uri' parameter does not match the authorization request"))
        }

        val accessTokenResult = accessTokens.create(code)

        return accessTokenResult.map { token ->
            Response(OK).let {
                when (codeDetails.responseType) {
                    Code -> it.body(token.value)
                    CodeIdToken -> {
                        val idToken = idTokens.createForAccessToken(code)
                        it.with(accessTokenResponseBody of AccessTokenResponse(token.value, idToken.value))
                    }
                }
            }
        }.mapFailure {
            Response(BAD_REQUEST).body(errorResponse(Error.invalid_grant, "The authorization code has already been used"))
        }.get()
    }

    private fun errorResponse(errorCode: Error, errorDescription: String) = json.asJsonString(ErrorResponse(errorCode, errorDescription, documentationUri))
}

enum class Error {
    invalid_grant,
    unsupported_grant_type,
    invalid_client,
    invalid_request,
    unauthorized_client
}

private data class ErrorResponse(val error: Error, val error_description: String, val error_uri: String)
