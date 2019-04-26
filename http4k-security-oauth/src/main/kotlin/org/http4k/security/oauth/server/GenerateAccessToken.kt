package org.http4k.security.oauth.server

import com.natpryce.Failure
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
import org.http4k.security.AccessTokenDetails
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
    private val json: AutoMarshallingJson,
    private val documentationUri: String = ""
) : HttpHandler {

    override fun invoke(request: Request): Response {
        val accessTokenRequest = request.accessTokenRequest()

        val accessTokenResult = generateAccessToken(accessTokenRequest)

        return accessTokenResult
            .map { token ->
                Response(OK).let {
                    when {
                        token.idToken == null -> it.body(token.accessToken.value)
                        else -> it.with(accessTokenResponseBody of AccessTokenResponse(token.accessToken.value, token.idToken.value))
                    }
                }
            }.mapFailure { error ->
                when (error) {
                    is InvalidClientCredentials -> Response(UNAUTHORIZED).withError(error.rfcError, error.description)
                    else -> Response(BAD_REQUEST).withError(error.rfcError, error.description)
                }
            }.get()
    }

    private fun generateAccessToken(accessTokenRequest: AccessTokenRequest) =
        when {
            accessTokenRequest.grantType != "authorization_code" -> Failure(UnsupportedGrantType(accessTokenRequest.grantType))
            !clientValidator.validateCredentials(accessTokenRequest.clientId, accessTokenRequest.clientSecret) -> Failure(InvalidClientCredentials)
            else -> {
                val code = accessTokenRequest.authorizationCode
                val codeDetails = authorizationCodes.detailsFor(code)

                when {
                    codeDetails.expiresAt.isBefore(clock.instant()) -> Failure(AuthorizationCodeExpired)
                    codeDetails.clientId != accessTokenRequest.clientId -> Failure(InvalidClientId)
                    codeDetails.redirectUri != accessTokenRequest.redirectUri -> Failure(InvalidRedirectUri)
                    else -> accessTokens.create(code)
                        .map { token ->
                            when (codeDetails.responseType) {
                                Code -> AccessTokenDetails(token)
                                CodeIdToken -> AccessTokenDetails(token, idTokens.createForAccessToken(code))
                            }
                        }
                }
            }
        }

    private fun Response.withError(error: String, errorDescription: String) =
        with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(json.asJsonString(ErrorResponse(error, errorDescription, documentationUri)))
}

// represents errors according to https://tools.ietf.org/html/rfc6749#section-5.2
sealed class AccessTokenError(val rfcError: String, val description: String)
private data class UnsupportedGrantType(val requestedGrantType: String) : AccessTokenError("unsupported_grant_type", "$requestedGrantType is not supported")
private object InvalidClientCredentials : AccessTokenError("invalid_client", "The 'client_id' parameter does not match the authorization request")
private object AuthorizationCodeExpired : AccessTokenError("invalid_grant", "The authorization code has expired")
private object InvalidClientId : AccessTokenError("invalid_grant", "The 'client_id' parameter does not match the authorization request")
private object InvalidRedirectUri : AccessTokenError("invalid_grant", "The 'redirect_uri' parameter does not match the authorization request")
object AuthorizationCodeAlreadyUsed : AccessTokenError("invalid_grant", "The authorization code has already been used")

data class ErrorResponse(val error: String, val error_description: String, val error_uri: String)
