package org.http4k.sse

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.security.Security

fun SseFilter(security: Security) = SseFilter { next ->
    {
        var targetRequest = it
        val authResponse = security.filter.then {
            targetRequest = it
            Response(OK)
        }(it)
        when {
            authResponse.status.successful -> next(targetRequest)
            else -> SseResponse(authResponse.status, authResponse.headers, true) { it.close() }
        }
    }
}
