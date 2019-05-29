package org.http4k.filter

import org.http4k.cloudnative.*
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNAUTHORIZED

/**
 * Handle exceptions from upstream calls and convert them into sensible server-side errors.
 * Optionally pass in a function to format the response body from the exception.
 */
fun ServerFilters.HandleUpstreamRequestFailed(
    exceptionToBody: UpstreamRequestFailed.() -> String = { localizedMessage }
): Filter {
    fun Status.toResponse(e: UpstreamRequestFailed) = Response(this).body(e.exceptionToBody())

    return Filter { next ->
        {
            try {
                next(it)
            } catch (e: UpstreamRequestFailed) {
                when (e.status) {
                    GATEWAY_TIMEOUT, CLIENT_TIMEOUT -> GATEWAY_TIMEOUT.toResponse(e)
                    NOT_FOUND -> NOT_FOUND.toResponse(e)
                    else -> SERVICE_UNAVAILABLE.toResponse(e)
                }
            }
        }
    }
}

/**
 * Convert upstream errors from upstream into exceptions which can be handled at a higher level.
 * Optionally pass in:
 * 1. a function to determine which responses are successful - defaults to status 200.299
 * 2. a function to format the exception message from the response.
 */
fun ClientFilters.HandleUpstreamRequestFailed(
    responseWasSuccessful: Response.() -> Boolean = { status.successful },
    responseToMessage: Response.() -> String = Response::bodyString
) = Filter { next ->
    {
        next(it).apply {
            if (!responseWasSuccessful())
                when (status) {
                    NOT_FOUND -> throw NotFound(responseToMessage())
                    CLIENT_TIMEOUT -> throw ClientTimeout(responseToMessage())
                    UNAUTHORIZED -> throw Unauthorized(responseToMessage())
                    GATEWAY_TIMEOUT -> throw GatewayTimeout(responseToMessage())
                    else -> throw UpstreamRequestFailed(status, responseToMessage())
                }
        }
    }
}