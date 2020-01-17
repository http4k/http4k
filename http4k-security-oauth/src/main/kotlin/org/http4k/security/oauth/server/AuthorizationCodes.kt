package org.http4k.security.oauth.server

import com.natpryce.Result
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.security.ResponseType
import org.http4k.security.ResponseType.Code
import org.http4k.security.openid.Nonce
import java.time.Instant

/**
 * Provides a consistent way to manage authorization codes
 */
interface AuthorizationCodes {
    /**
     * Create new authorization code to be given to client after the user successfully authorize access
     * The generated authorization code needs to be associated with the clientId and redirectUri for later verification.
     * It should also be associated with a given expire date (recommended to be shorter than 10 minutes)
     */
    fun create(request: Request, authRequest: AuthRequest, response: Response): Result<AuthorizationCode, UserRejectedRequest>

    /**
     * Retrieve the details of an authorization code
     */
    fun detailsFor(code: AuthorizationCode): AuthorizationCodeDetails
}

data class AuthorizationCodeDetails(
    val clientId: ClientId,
    val redirectUri: Uri,
    val expiresAt: Instant,
    val state: String?,
    val isOIDC: Boolean,
    val responseType: ResponseType = Code,
    val nonce: Nonce? = null
)
