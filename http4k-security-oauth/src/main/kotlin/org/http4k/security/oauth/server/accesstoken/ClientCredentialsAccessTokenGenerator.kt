package org.http4k.security.oauth.server.accesstoken

import org.http4k.core.Request
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.util.map

class ClientCredentialsAccessTokenGenerator(private val accessTokens: AccessTokens) : AccessTokenGenerator {
    override fun generate(request: Request, clientId: ClientId, tokenRequest: TokenRequest) =
        accessTokens.create(clientId, tokenRequest).map { AccessTokenDetails(it) }

}

