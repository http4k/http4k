package org.http4k.postbox

import dev.forkhandles.result4k.Result
import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.Path
import org.http4k.lens.asResult

interface Postbox {
    fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError>
    fun status(requestId: RequestId): Result<RequestProcessingStatus, PostboxError>
    fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError>
    fun pendingRequests(): List<PendingRequest>

    data class PendingRequest(val requestId: RequestId, val request: Request)
}

sealed class PostboxError(val description: String) {
    data object RequestNotFound : PostboxError("request not found")
    data class StorageFailure(val cause: Exception) : PostboxError("storage failed (cause: ${cause.message})")
    data class TransactionFailure(val cause: Exception) : PostboxError("transaction failed (cause: ${cause.message})")
    data class RequestProcessingFailure(val reason: String) : PostboxError(reason)
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
