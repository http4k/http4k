package org.http4k.postbox

import dev.forkhandles.result4k.asResultOr
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.db.Transactor
import org.http4k.db.performAsResult
import org.http4k.lens.location
import org.http4k.postbox.PendingResponseGenerators.Empty
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.routing.RoutedMessage
import org.http4k.routing.path

typealias RequestIdResolver = (Request) -> RequestId?

typealias PendingResponseGenerator = (RequestId) -> Response

/**
 * Configures HTTP handlers for a transactional postbox.
 */
class PostboxHandlers(
    private val transactor: Transactor<Postbox>,
    private val responseGenerator: PendingResponseGenerator = Empty
) {

    /**
     * The interceptor to be used in the application to store requests in the postbox.
     * It relies on the `requestIdResolver` to define the identity of the request and allow for idempotency.
     *
     * It'll return a 202 with a Link header to check the status of the request.
     * If the request has already been processed, it'll return the response obtained as part of processing it.
     */
    fun intercepting(resolver: RequestIdResolver): HttpHandler = { request: Request ->
        resolver(request).asResultOr { Response(BAD_REQUEST.description("request id not found")) }
            .flatMap { requestId ->
                transactor.performAsResult { it.store(Postbox.PendingRequest(requestId, request)) }
                    .mapFailure(PostboxError::TransactionFailure)
                    .flatMap { it }
                    .map { it.toResponse(requestId) }
                    .mapFailure { it.toResponse() }

            }.get()
    }

    /**
     * Allows to check the status of a request.
     *
     * If the request is pending, It'll return a 202 with a Link header to check the status of the request.
     * If the request has already been processed, it'll return the response obtained as part of processing it.
     * If the request is not found, it'll return a 404.
     */
    fun status(resolver: RequestIdResolver): HttpHandler = { request: Request ->
        resolver(request)
            .asResultOr { Response(BAD_REQUEST.description("request id not found")) }
            .flatMap { requestId ->
                transactor.performAsResult { postbox -> postbox.status(requestId) }
                    .mapFailure(PostboxError::TransactionFailure)
                    .flatMap { it }
                    .map { it.toResponse(requestId) }
                    .mapFailure { it.toResponse() }
            }.get()
    }


    private fun RequestProcessingStatus.toResponse(requestId: RequestId) = when (this) {
        is Pending -> responseGenerator(requestId)
        is Processed -> response
    }

    private fun PostboxError.toResponse() =
        when (this) {
            is PostboxError.RequestNotFound -> Response(NOT_FOUND.description(description))
            is PostboxError.StorageFailure -> Response(INTERNAL_SERVER_ERROR.description(description))
            is PostboxError.TransactionFailure -> Response(INTERNAL_SERVER_ERROR.description(description))
        }
}

object RequestIdResolvers {
    fun fromHeader(headerName: String) =
        { request: Request -> request.header(headerName)?.let(RequestId.Companion::of) }

    fun fromPath(pathName: String, uriTemplate: UriTemplate = UriTemplate.from("{$pathName}")) =
        { request: Request ->
            when (request) {
                is RoutedMessage -> request.path(pathName)
                else -> uriTemplate.extract(request.uri.path)[pathName]
            }?.let(RequestId.Companion::of)
        }
}

object PendingResponseGenerators {

    val Empty = { _: RequestId -> Response(ACCEPTED) }

    fun linkHeader(pathName: String, uriTemplate: UriTemplate = UriTemplate.from("/{$pathName}")) =
        { requestId: RequestId ->
            Response(ACCEPTED).header("Link", uriTemplate.generate(mapOf(pathName to requestId.value)))
        }

    fun redirect(pathName: String, uriTemplate: UriTemplate = UriTemplate.from("{$pathName}")) =
        { requestId: RequestId ->
            Response(FOUND).location(Uri.of(uriTemplate.generate(mapOf(pathName to requestId.value))))
        }
}


