package org.http4k.security.oauth.server.accesstoken

import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientValidator

interface AccessTokenRequestAuthentication {
    fun validateCredentials(request: Request): Boolean
}

class ClientSecretAccessTokenRequestAuthentication(private val clientValidator: ClientValidator) : AccessTokenRequestAuthentication {
    override fun validateCredentials(request: Request) =
            clientValidator.validateCredentials(
                    request,
                    ClientId(request.form("client_id").orEmpty()),
                    request.form("client_secret").orEmpty()
            )
}