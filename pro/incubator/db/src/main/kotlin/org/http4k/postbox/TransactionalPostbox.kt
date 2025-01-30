package org.http4k.postbox

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue
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
import org.http4k.lens.Path
import org.http4k.lens.asResult
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID

fun TransactionalPostbox(
    transactor: Transactor<Postbox>,
    requestIdResolver: (Request) -> RequestId = { RequestId.of(UUID.randomUUID().toString()) },
    statusTemplate: UriTemplate = UriTemplate.from("/postbox/{requestId}")
): HttpHandler {
    return { req: Request ->
        val requestId = requestIdResolver(req)
        transactor.performAsResult { it.store(requestId, req) }
            .mapFailure(PostboxError::TransactionFailure)
            .flatMap { it }
            .map { it.toResponse(requestId, statusTemplate) }
            .mapFailure { it.toResponse() }
            .get()
    }
}

fun PostboxHandler(
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
    }


interface Postbox {
    fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError>
    fun status(requestId: RequestId): Result<RequestProcessingStatus, PostboxError>
    fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError>
}

sealed class PostboxError(val description: String) {
    data object RequestNotFound : PostboxError("request not found")
    data class StorageFailure(val cause: Exception) : PostboxError("storage failed (cause: ${cause.message})")
    data class TransactionFailure(val cause: Exception) : PostboxError("transaction failed (cause: ${cause.message})")
}

sealed class RequestProcessingStatus {
    data object Pending : RequestProcessingStatus()
    data class Processed(val response: Response) : RequestProcessingStatus()
}

class RequestId private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<RequestId>(::RequestId) {
        val lens = Path.map(::RequestId).of("requestId").asResult()
    }
}
