package org.http4k.security.oauth.server

import org.http4k.security.AccessTokenContainer

class DummyAuthorizationCodes : AuthorizationCodes {
    override fun create() = AuthorizationCode("dummy-token")
}

class DummyAccessTokens : AccessTokens {
    override fun create() = AccessTokenContainer("dummy-access-token")
}