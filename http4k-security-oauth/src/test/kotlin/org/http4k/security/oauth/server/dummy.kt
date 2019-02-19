package org.http4k.security.oauth.server

import org.http4k.core.Uri
import org.http4k.security.AccessTokenContainer

class DummyAuthorizationCodes : AuthorizationCodes {
    override fun create() = AuthorizationCode("dummy-token")
}

class DummyAccessTokens : AccessTokens {
    override fun create() = AccessTokenContainer("dummy-access-token")
}

class DummyClientValidator : ClientValidator {
    override fun validate(clientId: ClientId, redirectionUri: Uri): Boolean = true
}

class HardcodedClientValidator(
    private val expectedClientId: ClientId,
    private val expectedRedirectionUri: Uri
) : ClientValidator {
    override fun validate(clientId: ClientId, redirectionUri: Uri) =
        clientId == this.expectedClientId && redirectionUri == this.expectedRedirectionUri
}