package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Result
import org.http4k.core.Request
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.OAuthError

interface AccessTokenGenerator<T : AccessTokenRequest> {
    val rfcGrantType: String
    fun resolveRequest(request: Request): Result<T, OAuthError>
    fun generate(request: T): Result<AccessTokenDetails, AccessTokenError>
}