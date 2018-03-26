package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN

interface OAuthPersistence {

    fun retrieveCsrf(p1: Request): CrossSiteRequestForgeryToken?

    fun withAssignedCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response

    fun retrieveToken(p1: Request): AccessToken?

    fun withAssignedToken(redirect: Response, accessToken: AccessToken): Response

    fun authFailureResponse() = Response(FORBIDDEN)
}