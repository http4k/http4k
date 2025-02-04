package org.http4k.postbox.inmemory

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus

class InMemoryPostbox : Postbox {
    private val requests = mutableMapOf<RequestId, Record>()

    private var fail = false

    fun failNext() {
        fail = true
    }

    private fun findRequest(requestId: RequestId) = requests[requestId]

    override fun store(pending: Postbox.PendingRequest): Result<RequestProcessingStatus, PostboxError> {
        return if (!fail) {
            val existingRequest = findRequest(pending.requestId)
            if (existingRequest == null) {
                requests[pending.requestId] = Record(pending.request)
                Success(RequestProcessingStatus.Pending)
            } else {
                val response = existingRequest.response
                if (response == null) {
                    Success(RequestProcessingStatus.Pending)
                } else {
                    Success(RequestProcessingStatus.Processed(response))
                }
            }
        } else {
            fail = false;
            Failure(PostboxError.StorageFailure(IllegalStateException("Failed to store request")))
        }
    }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> =
        findRequest(requestId)?.let {
            requests[requestId] = Record(it.request, response)
            Success(Unit)
        } ?: Failure(PostboxError.RequestNotFound)

    override fun markFailed(requestId: RequestId, response: Response?): Result<Unit, PostboxError> =
        findRequest(requestId)?.let {
            if(it.response != null && !it.failed) {
                return Failure(RequestAlreadyProcessed)
            }else{
            requests[requestId] = Record(it.request, it.response ?: response, failed = true)
            Success(Unit)}
        } ?: Failure(PostboxError.RequestNotFound)

    override fun status(requestId: RequestId) =
        findRequest(requestId)?.let {
            when {
                it.failed -> Success(RequestProcessingStatus.Failed(it.response))
                it.response != null -> Success(RequestProcessingStatus.Processed(it.response))
                else -> Success(RequestProcessingStatus.Pending)
            }
        } ?: Failure(PostboxError.RequestNotFound)

    override fun pendingRequests(batchSize: Int) = requests
        .filter { it.value.response == null }
        .map { Postbox.PendingRequest(it.key, it.value.request) }
        .toList()

    private data class Record(val request: Request, val response: Response? = null, val failed: Boolean = false)
}
