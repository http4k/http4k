package org.http4k.filter

import org.http4k.cloudnative.BadGateway
import org.http4k.cloudnative.BadRequest
import org.http4k.cloudnative.ClientTimeout
import org.http4k.cloudnative.Conflict
import org.http4k.cloudnative.InternalServerError
import org.http4k.cloudnative.NotFound
import org.http4k.cloudnative.ServiceUnavailable
import org.http4k.cloudnative.UpstreamRequestFailed
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONFLICT
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
            } catch (e: ClientTimeout) {
                GATEWAY_TIMEOUT.toResponse(e)
            } catch (e: NotFound) {
                NOT_FOUND.toResponse(e)
            } catch (e: UpstreamRequestFailed) {
                SERVICE_UNAVAILABLE.toResponse(e)
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
                    BAD_GATEWAY -> throw BadGateway(responseToMessage())
                    BAD_REQUEST -> throw BadRequest(responseToMessage())
                    CONFLICT -> throw Conflict(responseToMessage())
                    CLIENT_TIMEOUT -> throw ClientTimeout(responseToMessage())
                    GATEWAY_TIMEOUT -> throw ClientTimeout(responseToMessage())
                    SERVICE_UNAVAILABLE -> throw ServiceUnavailable(responseToMessage())
                    else -> throw InternalServerError(responseToMessage())
                }
        }
    }
}