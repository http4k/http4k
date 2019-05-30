package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Result
import org.http4k.core.Request
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError

interface AccessTokenGenerator {
    val rfcGrantType: String
    fun generate(request: Request): Result<AccessTokenDetails, AccessTokenError>
}