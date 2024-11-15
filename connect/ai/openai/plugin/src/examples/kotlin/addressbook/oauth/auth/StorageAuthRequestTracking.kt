package addressbook.oauth.auth

import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthRequestTracking
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Example AuthTrackingRequest generation and storage
 */
fun StorageAuthRequestTracking(
    storage: Storage<AuthRequest>,
    cookieDomain: String,
    clock: Clock,
    validity: Duration
) = object : AuthRequestTracking {
    private val cookieName = "t"

    override fun resolveAuthRequest(request: Request): AuthRequest? =
        request.cookie(cookieName)
            ?.value
            ?.let { storage[it]?.apply { storage.remove(it) } }

    override fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response) =
        UUID.randomUUID().toString().let {
            storage[it] = authRequest
            response.cookie(expiring(cookieName, it, validity))
        }

    protected fun expiring(name: String, value: String, duration: Duration) = Cookie(
        name,
        value,
        expires = clock.instant().plus(duration),
        domain = cookieDomain,
        path = "/"
    )
}
