package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import org.http4k.core.Request
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientValidator
import org.http4k.security.oauth.server.InvalidClientCredentials
import org.http4k.security.oauth.server.TokenRequest

interface AccessTokenRequestAuthentication {
    fun validateCredentials(request: Request, tokenRequest: TokenRequest): Result<Request, AccessTokenError>
}

class ClientSecretAccessTokenRequestAuthentication(private val clientValidator: ClientValidator) : AccessTokenRequestAuthentication {
    override fun validateCredentials(request: Request, tokenRequest: TokenRequest) =
            if (clientValidator.validateCredentials(request, tokenRequest.clientId ?:ClientId(""), tokenRequest.clientSecret ?: "")) {
                Success(request)
            } else {
                Failure(InvalidClientCredentials)
            }
}