package org.http4k.security.oauth.server.accesstoken

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodeExpired
import org.http4k.security.oauth.server.AuthorizationCodes
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientIdMismatch
import org.http4k.security.oauth.server.IdTokens
import org.http4k.security.oauth.server.InvalidPkceVerifier
import org.http4k.security.oauth.server.MissingAuthorizationCode
import org.http4k.security.oauth.server.MissingRedirectUri
import org.http4k.security.oauth.server.RedirectUriMismatch
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.secureEquals
import org.http4k.security.toS256Challenge
import java.time.Clock

class AuthorizationCodeAccessTokenGenerator(
    private val authorizationCodes: AuthorizationCodes,
    private val accessTokens: AccessTokens,
    private val clock: Clock,
    private val idTokens: IdTokens = IdTokens.Unsupported,
    private val requirePkce: Boolean = false
) : AccessTokenGenerator {
    override fun generate(request: Request, clientId: ClientId, tokenRequest: TokenRequest) =
        extract(clientId, tokenRequest).flatMap { generate(it) }

    fun generate(request: AuthorizationCodeAccessTokenRequest): Result<AccessTokenDetails, AccessTokenError> {
        val codeDetails = authorizationCodes.detailsFor(request.authorizationCode)

        return when {
            codeDetails.expiresAt.isBefore(clock.instant()) -> Failure(AuthorizationCodeExpired)
            codeDetails.clientId != request.clientId -> Failure(ClientIdMismatch)
            codeDetails.redirectUri != request.redirectUri -> Failure(RedirectUriMismatch)
            !pkceVerifierMatches(codeDetails.codeChallenge, request.codeVerifier) ->
                Failure(InvalidPkceVerifier)
            else -> accessTokens.create(codeDetails.clientId, request)
                .map { token ->
                    when {
                        codeDetails.isOIDC -> AccessTokenDetails(
                            token,
                            idTokens.createForAccessToken(codeDetails, request.authorizationCode, token)
                        )
                        else -> AccessTokenDetails(token)
                    }
                }
        }
    }

    private fun pkceVerifierMatches(challenge: String?, verifier: String?): Boolean {
        if (challenge == null) return !requirePkce
        if (verifier == null) return false
        return secureEquals(verifier.toS256Challenge(), challenge)
    }

    companion object {
        fun extract(
            clientId: ClientId,
            tokenRequest: TokenRequest
        ): Result<AuthorizationCodeAccessTokenRequest, AccessTokenError> {
            return Success(
                AuthorizationCodeAccessTokenRequest(
                    clientId = clientId,
                    clientSecret = tokenRequest.clientSecret ?: "",
                    redirectUri = tokenRequest.redirectUri ?: return Failure(MissingRedirectUri),
                    scopes = tokenRequest.scopes,
                    authorizationCode = AuthorizationCode(tokenRequest.code ?: return Failure(MissingAuthorizationCode)),
                    codeVerifier = tokenRequest.codeVerifier
                )
            )
        }
    }
}

data class AuthorizationCodeAccessTokenRequest(
    val clientId: ClientId,
    val clientSecret: String,
    val redirectUri: Uri,
    val scopes: List<String>,
    val authorizationCode: AuthorizationCode,
    val codeVerifier: String? = null
)
