package org.http4k.security.oauth.server

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Filter
import org.http4k.core.Request

fun AuthRequestTrackingFilter(
    tracking: AuthRequestTracking,
    extractor: AuthRequestExtractor,
    authoriseRequestErrorRender: AuthoriseRequestErrorRender
) = Filter { next ->
    { request: Request ->
        extractor.extract(request)
            .map {
                val response = next(request)
                tracking.trackAuthRequest(request, it, response)
            }.mapFailure { authoriseRequestErrorRender.errorFor(request, it) }.get()
    }
}
