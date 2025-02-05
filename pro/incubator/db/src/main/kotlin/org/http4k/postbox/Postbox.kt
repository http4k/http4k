package org.http4k.postbox

import dev.forkhandles.result4k.Result
import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.db.Transactor
import org.http4k.lens.Path
import org.http4k.lens.asResult
import java.time.Duration
import java.time.Instant

/**
 * Postbox is the storage mechanism for requests that are to be processed asynchronously.
 */
interface Postbox {
    /**
     * Store a request in the Postbox for later processing.
     *
     * @param pending the request to store, which includes an id and the request itself
     *
     * If the request is already stored, it will ignore the new value and return the status of the existing one.
     *
     * @return the status of the request processing
     *  - If the request is new or has not been processed, the status will be [RequestProcessingStatus.Pending]
     *  - If the request has been processed, the status will be [RequestProcessingStatus.Processed]
     *
     *  //TODO: test storing after processing/dead
     */
    fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError>

    /**
     * Retrieve the status of a request.
     *
     * @param requestId the id of the request to check
     *
     * @return the status of the request processing
     *   - If the request has not been processed, the status will be [RequestProcessingStatus.Pending]
     *   - If the request has been processed, the status will be [RequestProcessingStatus.Processed]
     *   - If the request is not found, the result will be a failure with [PostboxError.RequestNotFound]
     */
    fun status(requestId: RequestId): Result<RequestProcessingStatus, PostboxError>

    /**
     * Mark a request as processed with the given response.
     *
     * @return
     *  - If the request was successfully marked as processed, the result will be a success with [Unit]
     *  - If the request has been already processed or marked as failed, the result will be a failure with [PostboxError.StorageFailure]
     *  - If the request is not present, the result will be a failure with  [PostboxError.RequestNotFound]
     */
    fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError>

    /**
     * Mark a request as failed with the given delay for reprocessing and an optional response.
     *
     * @param requestId the id of the request to mark as failed
     * @param delayReprocessing the delay before reprocessing the request
     * @param response the response to store with the failed request (optional)
     *
     * If a response is provided, it overrides any previously stored one.
     *
     * @return
     *  - If the request was successfully marked as failed, the result will be a success with [Unit]
     *  - If the request is not found, the result will be a failure with [PostboxError.RequestNotFound]
     *  - If the request has been already processed or marked as dead, the result will be a failure with [PostboxError.StorageFailure]
     */
    fun markFailed(requestId: RequestId, delayReprocessing: Duration, response: Response?): Result<Unit, PostboxError>

    /**
     * Mark a request as permanently failed (dead) with an optional response.
     *
     * @param requestId the id of the request to mark as dead
     * @param response the response to store with the dead request (optional)
     *
     * If a response was not previously stored, the new response will be stored. Subsequent responses will be ignored.
     *
     * @return
     *   - If the request was successfully marked as dead, returns a success with [Unit]
     *   - If the request is not found, the result will be a failure with [PostboxError.RequestNotFound]
     *   - If the request has been already processed, the result will be a failure with [PostboxError.StorageFailure]
     */
    fun markDead(requestId: RequestId, response: Response? = null): Result<Unit, PostboxError>

    /**
     * Retrieve all pending requests. Those are the ones that have not been marked as processed or dead yet.
     *
     * It includes requests that have been delayed for reprocessing if they are due.
     *
     * @return a list of all pending requests in first-in-first-out order
     */
    fun pendingRequests(batchSize: Int, atTime: Instant): List<PendingRequest>

    data class PendingRequest(val requestId: RequestId, val request: Request, val processingTime: Instant)
}

sealed class PostboxError(val description: String) {
    data object RequestNotFound : PostboxError("request not found")
    data class StorageFailure(val cause: Exception) : PostboxError("storage failed (cause: ${cause.message})")
    data class TransactionFailure(val cause: Exception) : PostboxError("transaction failed (cause: ${cause.message})")

    companion object {
        val RequestAlreadyProcessed = StorageFailure(IllegalStateException("request already processed"))
        val RequestMarkedAsDead = StorageFailure(IllegalStateException("request already marked as dead"))
    }
}

sealed class RequestProcessingStatus {
    data object Pending : RequestProcessingStatus()
    data class Processed(val response: Response) : RequestProcessingStatus()
    data class Dead(val response: Response? = null) : RequestProcessingStatus()
}

class RequestId private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<RequestId>(::RequestId) {
        val lens = Path.map(::RequestId).of("requestId").asResult()
    }
}

typealias PostboxTransactor = Transactor<Postbox>
