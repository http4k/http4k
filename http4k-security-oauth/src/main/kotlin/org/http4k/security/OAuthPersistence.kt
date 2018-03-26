package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Uri

interface OAuthPersistence {
    fun retrieveCsrf(p1: Request): String?

    fun redirectAuth(redirect: Response, csrf: String): Response

    fun isAuthed(request: Request): Boolean

    fun redirectToken(redirect: Response, accessToken: String): Response

    fun modifyState(uri: Uri) = uri

    fun failedResponse() = Response(FORBIDDEN)
}