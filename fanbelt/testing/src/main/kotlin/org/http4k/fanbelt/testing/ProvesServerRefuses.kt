package org.http4k.fanbelt.testing

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.with
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.testing.Verdict.Denied
import org.http4k.fanbelt.testing.Verdict.Hidden
import org.http4k.fanbelt.testing.Verdict.Rejected
import org.http4k.lens.DATASTAR_REQUEST
import org.http4k.lens.Header
import org.http4k.lens.contentType
import org.http4k.testing.toHttpHandler

/**
 * Posts an action the page hides from this viewer and asserts the server refuses it.
 */
fun Node.provesServerRefuses(path: String, cookie: Cookie, signals: String = "{}") = handler.toHttpHandler().provesServerRefuses(path, listOf(cookie), signals)

/**
 * Posts an action as Datastar would and returns how the server refused it. Fails if the server accepts it.
 */
fun HttpHandler.provesServerRefuses(path: String, cookies: List<Cookie>, signals: String = "{}"): Verdict {
    val response = this(
        Request(POST, path)
        .with(Header.DATASTAR_REQUEST of true)
        .contentType(ContentType.APPLICATION_JSON)
            .cookies(cookies)
    )

    return when (response.status) {
        FORBIDDEN -> Denied

        NOT_FOUND -> Hidden

        else -> {
            val errors = Regex(""""errors":\{(.*?)}""").find(response.bodyString())?.groupValues?.get(1).orEmpty()
            val shown = Regex(""":"([^"]+)"""").findAll(errors).map { it.groupValues[1] }.toSet()
            check(shown.isNotEmpty()) { "$path is hidden from this viewer, but the server answered ${response.status}" }
            Rejected(shown)
        }
    }
}
