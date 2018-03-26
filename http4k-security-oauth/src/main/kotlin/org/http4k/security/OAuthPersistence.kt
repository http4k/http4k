package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN

interface OAuthPersistence {
    fun retrieveCsrf(p1: Request): String?

    fun redirectAuth(redirect: Response, csrf: String): Response

    fun hasToken(request: Request): Boolean

    fun redirectToken(redirect: Response, accessToken: String): Response

    fun failedResponse() = Response(FORBIDDEN)
}