package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.security.AccessTokenContainer
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.security.openid.IdTokenContainer
import java.time.Clock
import java.time.Instant
import java.util.UUID

open class DummyAuthorizationCodes(private val request: AuthRequest, private val shouldFail: (Request) -> Boolean, private val username: String? = null) : AuthorizationCodes {
    override fun create(request: Request, authRequest: AuthRequest, response: Response): Result<AuthorizationCode, UserRejectedRequest> = if (shouldFail(request)) Failure(UserRejectedRequest) else Success(AuthorizationCode("dummy-token-for-" + (username
        ?: "unknown")))

    override fun detailsFor(code: AuthorizationCode): AuthorizationCodeDetails = AuthorizationCodeDetails(request.client, request.redirectUri, Instant.EPOCH, request.responseType)
}

open class DummyIdtokens(private val username: String? = null) : IdTokens {
    override fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response) =
        IdTokenContainer("dummy-id-token-for-" + (username ?: "unknown"))

    override fun createForAccessToken(code: AuthorizationCode): IdTokenContainer =
        IdTokenContainer("dummy-id-token-for-access-token")
}

class DummyAccessTokens(val tokenIsValid: Boolean = true) : AccessTokens {
    override fun isValid(accessToken: AccessTokenContainer): Boolean = tokenIsValid

    override fun create(authorizationCode: AuthorizationCode) = Success(AccessTokenContainer("dummy-access-token"))
}

class ErroringAccessTokens(private val error: AuthorizationCodeAlreadyUsed) : AccessTokens {
    override fun isValid(accessToken: AccessTokenContainer): Boolean = true

    override fun create(authorizationCode: AuthorizationCode) = Failure(error)
}

class DummyClientValidator : ClientValidator {
    override fun validateClientId(clientId: ClientId): Boolean = true
    override fun validateCredentials(clientId: ClientId, clientSecret: String): Boolean = true
    override fun validateRedirection(clientId: ClientId, redirectionUri: Uri): Boolean = true
}

class DummyOAuthAuthRequestTracking : AuthRequestTracking {
    override fun trackAuthRequest(authRequest: AuthRequest, response: Response): Response = response
    override fun resolveAuthRequest(request: Request): AuthRequest? = request.authorizationRequest()
}

class HardcodedClientValidator(
    private val expectedClientId: ClientId,
    private val expectedRedirectionUri: Uri,
    private val expectedClientSecret: String = "secret for ${expectedClientId.value}"
) : ClientValidator {
    override fun validateClientId(clientId: ClientId): Boolean = clientId == this.expectedClientId

    override fun validateRedirection(clientId: ClientId, redirectionUri: Uri) =
        redirectionUri == this.expectedRedirectionUri

    override fun validateCredentials(clientId: ClientId, clientSecret: String) =
        clientId == expectedClientId && clientSecret == expectedClientSecret
}

class InMemoryAuthorizationCodes(private val clock: Clock) : AuthorizationCodes {
    private val codes = mutableMapOf<AuthorizationCode, AuthorizationCodeDetails>()
    private val usedCodes = mutableSetOf<AuthorizationCode>()

    override fun detailsFor(code: AuthorizationCode) =
        codes[code] ?: error("code not stored")

    override fun create(request: Request, authRequest: AuthRequest, response: Response): Result<AuthorizationCode, UserRejectedRequest> {
        return Success(AuthorizationCode(UUID.randomUUID().toString()).also {
            codes[it] = AuthorizationCodeDetails(authRequest.client, authRequest.redirectUri, clock.instant(), authRequest.responseType)
        })
    }

    fun available(authorizationCode: AuthorizationCode) = codes.containsKey(authorizationCode) && !usedCodes.contains(authorizationCode)
}

class InMemoryIdTokenConsumer : IdTokenConsumer {
    var consumedFromAuthorizationResponse: IdTokenContainer? = null
    var consumedFromAccessTokenResponse: IdTokenContainer? = null

    override fun consumeFromAuthorizationResponse(idToken: IdTokenContainer) {
        consumedFromAuthorizationResponse = idToken
    }

    override fun consumeFromAccessTokenResponse(idToken: IdTokenContainer) {
        consumedFromAccessTokenResponse = idToken
    }
}