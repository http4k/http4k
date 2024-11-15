package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies

const val FLASH_COOKIE = "flash-cookie"

object FlashAttributesFilter : Filter {
    override fun invoke(handler: HttpHandler) =
        { request: Request ->
            handler(request).let { response ->
                if (request.flash() != null && response.flash() == null)
                    response.removeFlash()
                else
                    response
            }
        }
}

fun Request.flash(): String? = cookie(FLASH_COOKIE)?.value
fun Request.withFlash(value: String): Request =
    cookie(Cookie(name = FLASH_COOKIE, value, maxAge = 3600, path = "/"))

fun Response.flash(): String? = cookies().firstOrNull { it.name == FLASH_COOKIE }?.value
fun Response.withFlash(value: String): Response =
    cookie(Cookie(name = FLASH_COOKIE, value, maxAge = 3600, path = "/"))

fun Response.removeFlash(): Response =
    cookie(Cookie(name = FLASH_COOKIE, "", maxAge = 0, path = "/"))
