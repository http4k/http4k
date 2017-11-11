package org.http4k.security

import org.http4k.core.Response
import org.http4k.core.Status
import org.pac4j.core.context.HttpConstants.FORBIDDEN
import org.pac4j.core.context.HttpConstants.OK
import org.pac4j.core.context.HttpConstants.TEMP_REDIRECT
import org.pac4j.core.context.HttpConstants.UNAUTHORIZED
import org.pac4j.core.http.HttpActionAdapter

class Http4kHttpActionAdapter : HttpActionAdapter<Response, Http4kWebContext> {

    override fun adapt(code: Int, context: Http4kWebContext): Response? =
        when (code) {
            UNAUTHORIZED -> Response(Status.UNAUTHORIZED)
            FORBIDDEN -> Response(Status.FORBIDDEN)
            OK -> context.response()
            TEMP_REDIRECT -> context.redirect()
            else -> Response(Status(code, "unknown"))
        }
}