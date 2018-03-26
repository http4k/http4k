package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN

data class CrossSiteRequestForgeryToken(val value: String)

data class AccessToken(val value: String)

interface OAuthPersistence {

    fun retrieveCsrf(p1: Request): String?

    fun assignCsrf(redirect: Response, csrf: String): Response

    fun retrieveToken(p1: Request): String?

    fun assignToken(redirect: Response, accessToken: String): Response

    fun authFailureResponse() = Response(FORBIDDEN)
}