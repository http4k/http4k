package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.security.AccessTokenContainer
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.security.openid.IdTokenContainer
import java.time.Clock
import java.time.Instant
import java.util.*

open class DummyAuthorizationCodes(private val request: AuthRequest) : AuthorizationCodes {
    override fun create(request: Request, authRequest: AuthRequest, response: Response): AuthorizationCode =
            AuthorizationCode("dummy-token-for-" + (response.header("user") ?: "unknown"))
    override fun detailsFor(code: AuthorizationCode): AuthorizationCodeDetails = AuthorizationCodeDetails(request.client, request.redirectUri, Instant.EPOCH, request.responseType)
    override fun destroy(authorizationCode: AuthorizationCode) = Unit
}

open class DummyIdtokens :IdTokens{
    override fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response) =
        IdTokenContainer("dummy-id-token-for-" + (response.header("user") ?: "unknown"))
}

class DummyAccessTokens : AccessTokens {
    override fun create(authorizationCode: AuthorizationCode) = AccessTokenContainer("dummy-access-token")
}

class DummyClientValidator : ClientValidator {
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
    override fun validateRedirection(clientId: ClientId, redirectionUri: Uri) =
        clientId == this.expectedClientId && redirectionUri == this.expectedRedirectionUri

    override fun validateCredentials(clientId: ClientId, clientSecret: String) =
        clientId == expectedClientId && clientSecret == expectedClientSecret
}

class InMemoryAuthorizationCodes(private val clock: Clock) : AuthorizationCodes {
    private val codes = mutableMapOf<AuthorizationCode, AuthorizationCodeDetails>()

    override fun detailsFor(code: AuthorizationCode) =
        codes[code] ?: error("code not stored")

    override fun create(request: Request, authRequest: AuthRequest, response: Response): AuthorizationCode {
        return AuthorizationCode(UUID.randomUUID().toString()).also {
            codes[it] = AuthorizationCodeDetails(authRequest.client, authRequest.redirectUri, clock.instant(), authRequest.responseType)
        }
    }

    override fun destroy(authorizationCode: AuthorizationCode) {
        codes.remove(authorizationCode)
    }

    fun available(authorizationCode: AuthorizationCode) = codes.containsKey(authorizationCode)
}

class InMemoryIdTokenConsumer : IdTokenConsumer {
    var consumed: IdTokenContainer? = null
    override fun consume(idToken: IdTokenContainer) {
        consumed = idToken
    }
}