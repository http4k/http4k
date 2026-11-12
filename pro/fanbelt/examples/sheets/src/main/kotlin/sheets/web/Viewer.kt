package sheets.web

import dev.forkhandles.values.parseOrNull
import dev.forkhandles.values.random
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import org.http4k.lens.RequestKey
import org.http4k.lens.location
import org.http4k.fanbelt.authentication.Authenticated
import sheets.domain.Viewer
import sheets.domain.ViewerId
import java.util.Random

private const val VIEWER_COOKIE = "viewer"

private fun Request.viewerId() = cookie(VIEWER_COOKIE)?.value?.let { ViewerId.parseOrNull(it) }

val principal = RequestKey.required<Viewer>("viewer")

val identified = Authenticated(principal, onRefused = { Response(SEE_OTHER).location(Uri.of("/")) }) { request -> request.viewerId()?.let(::Viewer) }

fun EnsureViewer(random: Random) = Filter { next ->
    {
        when (it.viewerId()) {
            null -> {
                val id = ViewerId.random(random)
                val cookie = viewerCookie(id)
                next(it).cookie(cookie)
            }

            else -> next(it)
        }
    }
}

fun viewerCookie(id: ViewerId) = Cookie(VIEWER_COOKIE, id.toString(), path = "/", httpOnly = true, sameSite = SameSite.Lax)
