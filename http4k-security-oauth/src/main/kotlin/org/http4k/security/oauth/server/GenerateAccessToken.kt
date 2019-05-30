package org.http4k.security.oauth.server

import com.natpryce.*
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
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
    private val errorRenderer: ErrorRenderer
) : HttpHandler {

    override fun invoke(request: Request): Response {
        return request.accessTokenRequest()
            .flatMap(this::generateAccessToken)
            .map { token ->
                Response(OK).let {
                    when {
                        token.idToken == null -> it.body(token.accessToken.value)
                        else -> it.with(accessTokenResponseBody of AccessTokenResponse(token.accessToken.value, token.idToken.value))
                    }
                }
            }.mapFailure(errorRenderer::response).get()
    }

    private fun generateAccessToken(accessTokenRequest: AccessTokenRequest) =
        when (accessTokenRequest) {
            is ClientCredentialsTokenRequest -> Failure(UnsupportedGrantType("client_credentials"))
            is AuthorizationCodeAccessTokenRequest -> generateAccessTokenForAuthorizationCode(accessTokenRequest)
        }

    private fun generateAccessTokenForAuthorizationCode(accessTokenRequest: AuthorizationCodeAccessTokenRequest) =
        when {
            accessTokenRequest.grantType != "authorization_code" -> Failure(UnsupportedGrantType(accessTokenRequest.grantType))
            !clientValidator.validateCredentials(accessTokenRequest.clientId, accessTokenRequest.clientSecret) -> Failure(InvalidClientCredentials)
            else -> {
                val code = accessTokenRequest.authorizationCode
                val codeDetails = authorizationCodes.detailsFor(code)

                when {
                    codeDetails.expiresAt.isBefore(clock.instant()) -> Failure(AuthorizationCodeExpired)
                    codeDetails.clientId != accessTokenRequest.clientId -> Failure(ClientIdMismatch)
                    codeDetails.redirectUri != accessTokenRequest.redirectUri -> Failure(RedirectUriMismatch)
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
}