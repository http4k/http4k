package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request

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
            }.mapFailure { authoriseRequestErrorRender.errorFor(request, it) }.get()
    }
}
