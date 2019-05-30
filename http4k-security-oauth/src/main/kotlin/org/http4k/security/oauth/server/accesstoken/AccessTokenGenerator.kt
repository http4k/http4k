package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Result
import org.http4k.core.Request
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError

interface AccessTokenGenerator<T : AccessTokenRequest> {
    val rfcGrantType: String
    fun resolveRequest(request: Request): Result<T, AccessTokenError>
    fun generate(request: T): Result<AccessTokenDetails, AccessTokenError>
}