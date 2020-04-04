package org.http4k.security.oauth.server.accesstoken

import org.http4k.core.Request
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientValidator
import org.http4k.security.oauth.server.InvalidClientCredentials
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.util.Failure
import org.http4k.util.Result
import org.http4k.util.Success

interface AccessTokenRequestAuthentication {
    fun validateCredentials(request: Request, tokenRequest: TokenRequest): Result<AccessTokenError, Triple<Request, ClientId, TokenRequest>>
}

class ClientSecretAccessTokenRequestAuthentication(private val clientValidator: ClientValidator) : AccessTokenRequestAuthentication {
    override fun validateCredentials(request: Request, tokenRequest: TokenRequest): Result<AccessTokenError, Triple<Request, ClientId, TokenRequest>> {
        val clientId = tokenRequest.clientId ?: ClientId("")
        return if (clientValidator.validateCredentials(request, clientId, tokenRequest.clientSecret ?: "")) {
            Success(Triple(request, clientId, tokenRequest))
        } else {
            Failure(InvalidClientCredentials)
        }
    }
}
