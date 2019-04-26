package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Header
import org.http4k.security.AccessTokenResponse
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.server.AccessTokenError.InvalidClient
import org.http4k.security.oauth.server.AccessTokenError.InvalidGrant
import org.http4k.security.oauth.server.AccessTokenError.UnsupportedGrantType
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
            return Response(BAD_REQUEST).withError(UnsupportedGrantType, "${accessTokenRequest.grantType} is not supported")
        }

        if (!clientValidator.validateCredentials(accessTokenRequest.clientId, accessTokenRequest.clientSecret)) {
            return Response(UNAUTHORIZED).withError(InvalidClient, "Client id and secret unrecognised")
        }

        val code = accessTokenRequest.authorizationCode
        val codeDetails = authorizationCodes.detailsFor(code)

        if (codeDetails.expiresAt.isBefore(clock.instant())) {
            return Response(BAD_REQUEST).withError(InvalidGrant, "The authorization code has expired")
        }

        if (codeDetails.clientId != accessTokenRequest.clientId) {
            return Response(BAD_REQUEST).withError(InvalidGrant, "The 'client_id' parameter does not match the authorization request")
        }

        if (codeDetails.redirectUri != accessTokenRequest.redirectUri) {
            return Response(BAD_REQUEST).withError(InvalidGrant, "The 'redirect_uri' parameter does not match the authorization request")
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
            Response(BAD_REQUEST).withError(InvalidGrant, "The authorization code has already been used")
        }.get()
    }

    private fun Response.withError(error: AccessTokenError, errorDescription: String) =
        with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(json.asJsonString(ErrorResponse(error.rfcValue, errorDescription, documentationUri)))

}

// represents errors according to https://tools.ietf.org/html/rfc6749#section-5.2
private enum class AccessTokenError(val rfcValue: String) {
    InvalidGrant("invalid_grant"),
    UnsupportedGrantType("unsupported_grant_type"),
    InvalidClient("invalid_client"),
    InvalidRequest("invalid_request"),
    UnauthorizedClient("unauthorized_client")
}

private data class ErrorResponse(val error: String, val error_description: String, val error_uri: String)
