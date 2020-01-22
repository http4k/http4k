package org.http4k.security.oauth.server.request

import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidAuthorizationRequest
import org.http4k.security.openid.RequestJwtContainer
import java.lang.UnsupportedOperationException

interface RequestValidator {

    fun validate(clientId: ClientId, requestJwtContainer: RequestJwtContainer): InvalidAuthorizationRequest?

    companion object {
        val Unsupported = object : RequestValidator {
            override fun validate(clientId: ClientId, requestJwtContainer: RequestJwtContainer): InvalidAuthorizationRequest? {
                throw UnsupportedOperationException("Request JWTs are not supported by this server")
            }

        }
    }
}
