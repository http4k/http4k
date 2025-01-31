package org.http4k.postbox

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.db.Transactor
import org.http4k.db.performAsResult
import java.util.*

class PostboxHandlers(
    private val transactor: Transactor<Postbox>,
    private val requestIdResolver: (Request) -> RequestId  = { RequestId.of(UUID.randomUUID().toString()) },
    private val statusTemplate: UriTemplate = UriTemplate.from("/postbox/{requestId}")
) {
    val interceptor: HttpHandler = { request: Request ->
        val requestId = requestIdResolver(request)
        transactor.performAsResult { it.store(Postbox.PendingRequest(requestId, request)) }
            .mapFailure(PostboxError::TransactionFailure)
            .flatMap { it }
            .map { it.toResponse(requestId, statusTemplate) }
            .mapFailure { it.toResponse() }
            .get()
    }

    val status: HttpHandler = { req: Request ->
        RequestId.lens(req)
            .mapFailure { Response(BAD_REQUEST.description(it.message.orEmpty())) }
            .flatMap { requestId ->
                transactor.performAsResult { postbox -> postbox.status(requestId) }
                    .mapFailure(PostboxError::TransactionFailure)
                    .flatMap { it }
                    .map { it.toResponse(requestId, statusTemplate) }
                    .mapFailure { it.toResponse() }
            }.get()
    }


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
        }
}


