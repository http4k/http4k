package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.security.AccessToken
import org.http4k.security.Nonce
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest
import org.http4k.security.oauth.core.RefreshToken
import org.http4k.security.oauth.server.refreshtoken.RefreshTokens
import org.http4k.security.openid.IdToken
import org.http4k.security.openid.IdTokenConsumer
import java.time.Clock
import java.time.Instant
import java.util.UUID

class DummyAuthorizationCodes(private val request: AuthRequest, private val shouldFail: (Request) -> Boolean, private val username: String? = null) : AuthorizationCodes {
    override fun create(request: Request, authRequest: AuthRequest, response: Response): Result<AuthorizationCode, UserRejectedRequest> = if (shouldFail(request)) Failure(UserRejectedRequest) else Success(AuthorizationCode("dummy-token-for-" + (username
        ?: "unknown")))

    override fun detailsFor(code: AuthorizationCode): AuthorizationCodeDetails = AuthorizationCodeDetails(request.client, request.redirectUri!!, Instant.EPOCH, request.state, request.isOIDC(), request.responseType, request.nonce)
}

class DummyIdTokens(private val username: String? = null) : IdTokens {

    override fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response,
                                        nonce: Nonce?, code: AuthorizationCode) =
        IdToken("dummy-id-token-for-" + (username ?: "unknown") + "-nonce:" + (nonce?.value ?: "unknown"))

    override fun createForAccessToken(authorizationCodeDetails: AuthorizationCodeDetails, code: AuthorizationCode, accessToken: AccessToken): IdToken =
        IdToken("dummy-id-token-for-access-token")
}

class DummyAccessTokens : AccessTokens {
    override fun create(clientId: ClientId, tokenRequest: TokenRequest): Result<AccessToken, AccessTokenError> = Success(AccessToken("dummy-access-token"))

    override fun create(clientId: ClientId, tokenRequest: AuthorizationCodeAccessTokenRequest, authorizationCode: AuthorizationCode) = Success(AccessToken("dummy-access-token"))
}

class DummyRefreshTokens : RefreshTokens {
    override fun refreshAccessToken(clientId: ClientId, tokenRequest: TokenRequest, refreshToken: RefreshToken): Result<AccessToken, AccessTokenError> {
        return if (refreshToken.value.startsWith("valid")) {
            Success(newAccessToken)
        } else {
            Failure(InvalidRequest("Invalid refresh token"))
        }
    }

    companion object {
        val newAccessToken = AccessToken(
            "new-valid-access-token",
            expiresIn = 100,
            scope = "openid",
            refreshToken = RefreshToken("new-valid-refresh-token")
        )
    }
}

class ErroringAccessTokens(private val error: AuthorizationCodeAlreadyUsed) : AccessTokens {
    override fun create(clientId: ClientId, tokenRequest: TokenRequest): Result<AccessToken, AccessTokenError> = Failure(error)

    override fun create(clientId: ClientId, tokenRequest: AuthorizationCodeAccessTokenRequest, authorizationCode: AuthorizationCode) = Failure(error)
}

class DummyClientValidator : ClientValidator {
    override fun validateClientId(request: Request, clientId: ClientId): Boolean = true
    override fun validateCredentials(request: Request, clientId: ClientId, clientSecret: String): Boolean = true
    override fun validateRedirection(request: Request, clientId: ClientId, redirectionUri: Uri): Boolean = true
    override fun validateScopes(request: Request, clientId: ClientId, scopes: List<String>): Boolean = true
}

class DummyOAuthAuthRequestTracking : AuthRequestTracking {
    override fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response): Response = response
    override fun resolveAuthRequest(request: Request): AuthRequest? = request.authorizationRequest()
}

class HardcodedClientValidator(
    private val expectedClientId: ClientId,
    private val expectedRedirectionUri: Uri,
    private val expectedClientSecret: String = "secret for ${expectedClientId.value}",
    private val expectedScopes: List<String> = emptyList()
) : ClientValidator {
    override fun validateClientId(request: Request, clientId: ClientId): Boolean = clientId == this.expectedClientId

    override fun validateRedirection(request: Request, clientId: ClientId, redirectionUri: Uri) =
        redirectionUri == this.expectedRedirectionUri

    override fun validateCredentials(request: Request, clientId: ClientId, clientSecret: String) =
        clientId == expectedClientId && clientSecret == expectedClientSecret

    override fun validateScopes(request: Request, clientId: ClientId, scopes: List<String>): Boolean =
        scopes.toSet() == expectedScopes.toSet()
}

class InMemoryAuthorizationCodes(private val clock: Clock) : AuthorizationCodes {
    private val codes = mutableMapOf<AuthorizationCode, AuthorizationCodeDetails>()

    override fun detailsFor(code: AuthorizationCode) = codes[code] ?: error("code not stored")

    override fun create(request: Request, authRequest: AuthRequest, response: Response) =
        Success(AuthorizationCode(UUID.randomUUID().toString()).also {
            codes[it] = AuthorizationCodeDetails(authRequest.client, authRequest.redirectUri!!, clock.instant(), authRequest.state, authRequest.isOIDC(), authRequest.responseType, authRequest.nonce)
        })
}

class InMemoryIdTokenConsumer(var expectedNonce: Nonce? = null) : IdTokenConsumer {
    var consumedFromAuthorizationResponse: IdToken? = null
    var consumedFromAccessTokenResponse: IdToken? = null

    override fun nonceFromIdToken(idToken: IdToken): Nonce? = expectedNonce

    override fun consumeFromAuthorizationResponse(idToken: IdToken) {
        consumedFromAuthorizationResponse = idToken
    }

    override fun consumeFromAccessTokenResponse(idToken: IdToken) {
        consumedFromAccessTokenResponse = idToken
    }
}
