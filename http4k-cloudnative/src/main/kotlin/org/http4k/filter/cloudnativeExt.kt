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
 */
fun ServerFilters.HandleUpstreamRequestFailed() = Filter { next ->
    {
        try {
            next(it)
        } catch (e: Conflict) {
            SERVICE_UNAVAILABLE.toResponse(e)
        } catch (e: BadRequest) {
            SERVICE_UNAVAILABLE.toResponse(e)
        } catch (e: InternalServerError) {
            SERVICE_UNAVAILABLE.toResponse(e)
        } catch (e: BadGateway) {
            SERVICE_UNAVAILABLE.toResponse(e)
        } catch (e: ServiceUnavailable) {
            SERVICE_UNAVAILABLE.toResponse(e)
        } catch (e: ClientTimeout) {
            GATEWAY_TIMEOUT.toResponse(e)
        } catch (e: NotFound) {
            NOT_FOUND.toResponse(e)
        }
    }
}

private fun Status.toResponse(e: UpstreamRequestFailed) = Response(this).body(e.message ?: "")

/**
 * Convert upstream errors from upstream into exceptions which can be handled at a higher level.
 */
fun ClientFilters.HandleUpstreamRequestFailed(
    notFoundIsExpected: Boolean = true,
    toBody: Response.() -> String = Response::bodyString
) = Filter { next ->
    {
        next(it).apply {
            if (!status.successful)
                when (status) {
                    NOT_FOUND -> if (!notFoundIsExpected) throw NotFound(toBody())
                    BAD_GATEWAY -> throw BadGateway(toBody())
                    BAD_REQUEST -> throw BadRequest(toBody())
                    CONFLICT -> throw Conflict(toBody())
                    CLIENT_TIMEOUT -> throw ClientTimeout(toBody())
                    GATEWAY_TIMEOUT -> throw ClientTimeout(toBody())
                    SERVICE_UNAVAILABLE -> throw ServiceUnavailable(toBody())
                    else -> throw InternalServerError(toBody())
                }
        }
    }
}