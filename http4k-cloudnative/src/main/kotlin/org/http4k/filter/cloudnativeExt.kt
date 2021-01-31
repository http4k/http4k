package org.http4k.filter

import org.http4k.cloudnative.ClientTimeout
import org.http4k.cloudnative.Forbidden
import org.http4k.cloudnative.GatewayTimeout
import org.http4k.cloudnative.NotFound
import org.http4k.cloudnative.RemoteRequestFailed
import org.http4k.cloudnative.Unauthorized
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNAUTHORIZED

/**
 * Handle exceptions from remote calls and convert them into sensible server-side errors.
 * Optionally pass in a function to format the response body from the exception.
 */
fun ServerFilters.HandleRemoteRequestFailed(
    exceptionToBody: RemoteRequestFailed.() -> String = Throwable::getLocalizedMessage
): Filter {
    fun Status.toResponse(e: RemoteRequestFailed) = Response(this).body(e.exceptionToBody())

    return Filter { next ->
        {
            try {
                next(it)
            } catch (e: RemoteRequestFailed) {
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
 * Convert errors from remote calls into exceptions which can be handled at a higher level.
 * Optionally pass in:
 * 1. a function to determine which responses are successful - defaults to status 200..299
 * 2. a function to format the exception message from the response.
 */

fun ClientFilters.HandleRemoteRequestFailed(
    responseWasSuccessful: Response.() -> Boolean = { status.successful },
    responseToMessage: Response.() -> String = Response::bodyString
) = Filter { next ->
    {
        next(it).apply {
            if (!responseWasSuccessful())
                when (status) {
                    NOT_FOUND -> throw NotFound(responseToMessage(), it.uri)
                    CLIENT_TIMEOUT -> throw ClientTimeout(responseToMessage(), it.uri)
                    FORBIDDEN -> throw Forbidden(responseToMessage(), it.uri)
                    UNAUTHORIZED -> throw Unauthorized(responseToMessage(), it.uri)
                    GATEWAY_TIMEOUT -> throw GatewayTimeout(responseToMessage(), it.uri)
                    else -> throw RemoteRequestFailed(status, responseToMessage(), it.uri)
                }
        }
    }
}
