package org.http4k.filter

import org.http4k.cloudnative.ClientTimeout
import org.http4k.cloudnative.GatewayTimeout
import org.http4k.cloudnative.NotFound
import org.http4k.cloudnative.UpstreamRequestFailed
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE

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
 * Optionally pass in a function to format the exception message from the response.
 */
fun ClientFilters.HandleUpstreamRequestFailed(
    notFoundIsAcceptable: Boolean = true,
    responseToMessage: Response.() -> String = Response::bodyString
) = Filter { next ->
    {
        next(it).apply {
            if (!status.successful)
                when (status) {
                    NOT_FOUND -> if (!notFoundIsAcceptable) throw NotFound(responseToMessage())
                    CLIENT_TIMEOUT -> throw ClientTimeout(responseToMessage())
                    GATEWAY_TIMEOUT -> throw GatewayTimeout(responseToMessage())
                    else -> throw UpstreamRequestFailed(status, responseToMessage())
                }
        }
    }
}