package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.body.form

interface AccessTokenFetcherAuthenticator {
    fun authenticate(request: Request): Request
}

class ClientSecretAccessTokenFetcherAuthenticator(private val providerConfig: OAuthProviderConfig) : AccessTokenFetcherAuthenticator {
    override fun authenticate(request: Request) = request.form("client_secret", providerConfig.credentials.password)
}
