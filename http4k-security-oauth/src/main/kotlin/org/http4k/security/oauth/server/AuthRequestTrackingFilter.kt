package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.util.map
import org.http4k.util.recover

class AuthRequestTrackingFilter(
    private val tracking: AuthRequestTracking,
    private val extractor: AuthRequestExtractor,
    private val authoriseRequestErrorRender: AuthoriseRequestErrorRender
) : Filter {
    override fun invoke(next: HttpHandler) = { request: Request ->
        extractor.extract(request)
            .map {
                val response = next(request)
                tracking.trackAuthRequest(request, it, response)
            }.recover { authoriseRequestErrorRender.errorFor(request, it) }
    }
}
