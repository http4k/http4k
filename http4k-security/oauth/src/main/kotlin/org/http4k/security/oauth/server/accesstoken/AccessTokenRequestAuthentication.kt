package org.http4k.security.oauth.server.accesstoken

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.core.ClientId
import org.http4k.security.oauth.server.ClientValidator
import org.http4k.security.oauth.server.InvalidClientCredentials
import org.http4k.security.oauth.server.TokenRequest

interface AccessTokenRequestAuthentication {
    fun validateCredentials(request: Request, tokenRequest: TokenRequest): Result<Triple<Request, ClientId, TokenRequest>, AccessTokenError>
}

class ClientSecretAccessTokenRequestAuthentication(private val clientValidator: ClientValidator) : AccessTokenRequestAuthentication {
    override fun validateCredentials(request: Request, tokenRequest: TokenRequest): Result<Triple<Request, ClientId, TokenRequest>, AccessTokenError> {
        val clientId = tokenRequest.clientId ?: ClientId("")
        return if (clientValidator.validateCredentials(request, clientId, tokenRequest.clientSecret ?: "")) {
            Success(Triple(request, clientId, tokenRequest))
        } else {
            Failure(InvalidClientCredentials)
        }
    }
}
