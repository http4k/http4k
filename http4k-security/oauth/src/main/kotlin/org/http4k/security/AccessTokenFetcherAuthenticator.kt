package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.filter.ClientFilters.CustomBasicAuth.withBasicAuth

fun interface AccessTokenFetcherAuthenticator {
    fun authenticate(request: Request): Request
}

class ClientSecretAccessTokenFetcherAuthenticator(private val providerConfig: OAuthProviderConfig) : AccessTokenFetcherAuthenticator {
    override fun authenticate(request: Request) = request.form("client_secret", providerConfig.credentials.password)
}

class BasicAuthAccessTokenFetcherAuthenticator(private val providerConfig: OAuthProviderConfig) :
    AccessTokenFetcherAuthenticator {
    override fun authenticate(request: Request) = request.withBasicAuth(providerConfig.credentials, "Authorization")
}

