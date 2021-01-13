package org.http4k.security.oauth.server.request

import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidAuthorizationRequest
import org.http4k.security.openid.RequestJwtContainer

fun interface RequestJWTValidator {

    fun validate(clientId: ClientId, requestJwtContainer: RequestJwtContainer): InvalidAuthorizationRequest?

    companion object {
        val Unsupported = RequestJWTValidator { _, _ -> throw UnsupportedOperationException("Request JWTs are not supported by this server") }
    }
}
