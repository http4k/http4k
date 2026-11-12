package sl4k.web

import dev.forkhandles.values.parseOrNull
import dev.forkhandles.values.random
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import org.http4k.lens.RequestKey
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import java.util.Random

private const val VIEWER_COOKIE = "viewer"

fun Request.viewerId() = cookie(VIEWER_COOKIE)?.value?.let { ViewerId.parseOrNull(it) }

val principal = RequestKey.required<Viewer>("viewer")

fun EnsureViewer(random: Random) = Filter { next ->
    { if (it.viewerId() == null) next(it).cookie(viewerCookie(ViewerId.random(random))) else next(it) }
}

fun viewerCookie(id: ViewerId) = Cookie(VIEWER_COOKIE, id.toString(), path = "/", httpOnly = true, sameSite = SameSite.Lax)