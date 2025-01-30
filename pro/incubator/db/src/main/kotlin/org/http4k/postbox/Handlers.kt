package org.http4k.postbox

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.db.Transactor
import org.http4k.db.performAsResult
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.*

fun PostboxInterceptorHandler(
    transactor: Transactor<Postbox>,
    requestIdResolver: (Request) -> RequestId = { RequestId.of(UUID.randomUUID().toString()) },
    statusTemplate: UriTemplate = UriTemplate.from("/postbox/{requestId}")
): HttpHandler = { req: Request ->
    val requestId = requestIdResolver(req)
    transactor.performAsResult { it.store(requestId, req) }
        .mapFailure(PostboxError::TransactionFailure)
        .flatMap { it }
        .map { it.toResponse(requestId, statusTemplate) }
        .mapFailure { it.toResponse() }
        .get()
}

fun PostboxStatusHandler(
    transactor: Transactor<Postbox>,
    statusTemplate: UriTemplate = UriTemplate.from("/postbox/{requestId}")
): RoutingHttpHandler =
    routes(statusTemplate.toString() bind GET to { req: Request ->
        RequestId.lens(req)
            .mapFailure { Response(BAD_REQUEST.description(it.message.orEmpty())) }
            .flatMap { requestId ->
                transactor.performAsResult { postbox -> postbox.status(requestId) }
                    .mapFailure(PostboxError::TransactionFailure)
                    .flatMap { it }
                    .map { it.toResponse(requestId, statusTemplate) }
                    .mapFailure { it.toResponse() }
            }.get()
    })

private fun RequestProcessingStatus.toResponse(requestId: RequestId, statusTemplate: UriTemplate) = when (this) {
    RequestProcessingStatus.Pending ->
        Response(ACCEPTED).header("Link", statusTemplate.generate(mapOf("requestId" to requestId.value)))

    is RequestProcessingStatus.Processed -> response
}

private fun PostboxError.toResponse() =
    when (this) {
        is PostboxError.RequestNotFound -> Response(NOT_FOUND.description(description))
        is PostboxError.StorageFailure -> Response(INTERNAL_SERVER_ERROR.description(description))
        is PostboxError.TransactionFailure -> Response(INTERNAL_SERVER_ERROR.description(description))
        is PostboxError.RequestProcessingFailure -> Response(INTERNAL_SERVER_ERROR.description(description))
    }



