package org.http4k.connect.openai.auth.oauth.internal

import dev.forkhandles.result4k.peek
import org.http4k.connect.openai.auth.oauth.PrincipalChallenge
import org.http4k.connect.openai.auth.oauth.PrincipalStore
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodes

internal fun <Principal : Any> PluginAuthorizationCodes(
    authorizationCodes: AuthorizationCodes,
    principalStore: PrincipalStore<Principal>,
    principalChallenge: PrincipalChallenge<Principal>
) = object : AuthorizationCodes {
    override fun create(
        request: Request,
        authRequest: AuthRequest,
        response: Response
    ) = authorizationCodes.create(request, authRequest, response)
        .peek { principalStore[it] = principalChallenge[request] }

    override fun detailsFor(code: AuthorizationCode) = authorizationCodes.detailsFor(code)
}
