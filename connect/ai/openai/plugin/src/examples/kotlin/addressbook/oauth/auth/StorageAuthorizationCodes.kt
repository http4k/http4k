package addressbook.oauth.auth

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.peek
import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodeDetails
import org.http4k.security.oauth.server.AuthorizationCodes
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Example AuthorizationCode generation and storage
 */
fun StorageAuthorizationCodes(
    authDetailsStorage: Storage<AuthorizationCodeDetails>,
    clock: Clock,
    validity: Duration
) = object : AuthorizationCodes {
    override fun create(
        request: Request,
        authRequest: AuthRequest,
        response: Response
    ) = Success(AuthorizationCode(UUID.randomUUID().toString()))
        .peek {
            authDetailsStorage[it.value] = AuthorizationCodeDetails(
                authRequest.client, authRequest.redirectUri!!, clock.instant() + validity, null, false,
                authRequest.responseType
            )
        }

    override fun detailsFor(code: AuthorizationCode) = authDetailsStorage[code.value]
        ?.also { authDetailsStorage.remove(code.value) }
        ?: error("unknown code")
}
