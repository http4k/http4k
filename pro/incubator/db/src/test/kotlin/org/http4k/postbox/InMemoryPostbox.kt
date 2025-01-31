package org.http4k.postbox

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Response

class InMemoryPostbox : Postbox {
    private val requests = mutableMapOf<RequestId, Pair<Request, Response?>>()

    private var fail = false

    fun failNext() {
        fail = true
    }

    private fun findRequest(requestId: RequestId) = requests[requestId]

    override fun store(pending: Postbox.PendingRequest): Result<RequestProcessingStatus, PostboxError> {
        return if (!fail) {
            val existingRequest = findRequest(pending.requestId)
            if (existingRequest == null) {
                requests[pending.requestId] = pending.request to null
                Success(RequestProcessingStatus.Pending)
            } else {
                val response = existingRequest.second
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
            requests[requestId] = it.first to response
            Success(Unit)
        } ?: Failure(PostboxError.RequestNotFound)

    override fun status(requestId: RequestId) =
        findRequest(requestId)?.let {
            when {
                it.second != null -> Success(RequestProcessingStatus.Processed(it.second!!))
                else -> Success(RequestProcessingStatus.Pending)
            }
        } ?: Failure(PostboxError.RequestNotFound)

    override fun pendingRequests() = requests
        .filter { it.value.second == null }
        .map { Postbox.PendingRequest(it.key, it.value.first) }
        .toList()
}
