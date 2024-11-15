package org.http4k.security.oauth.server.accesstoken

import dev.forkhandles.result4k.Result
import org.http4k.core.Request
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest

fun interface AccessTokenGenerator {
    fun generate(
        request: Request,
        clientId: ClientId,
        tokenRequest: TokenRequest
    ): Result<AccessTokenDetails, AccessTokenError>
}
