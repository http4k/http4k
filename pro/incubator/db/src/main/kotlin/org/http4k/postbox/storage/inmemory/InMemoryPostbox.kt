package org.http4k.postbox.storage.inmemory

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.TimeSource
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.storage.inmemory.InMemoryPostbox.Status.*
import java.time.Duration
import java.time.Instant

class InMemoryPostbox(val timeSource: TimeSource) : Postbox {
    private val requests = mutableMapOf<RequestId, Record>()

    private var fail = false

    fun failNext() {
        fail = true
    }

    private fun findRequest(requestId: RequestId) = requests[requestId]

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> {
        return if (!fail) {
            val now = timeSource()
            val existingRequest = findRequest(requestId)
            if (existingRequest == null) {
                requests[requestId] = Record(now, request)
                Success(RequestProcessingStatus.Pending(0, now))
            } else {
                val response = existingRequest.response
                if (response == null) {
                    Success(RequestProcessingStatus.Pending(existingRequest.failures, existingRequest.processAt))
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
            when (it.status) {
                PENDING -> {
                    requests[requestId] = Record(it.processAt, it.request, it.response ?: response, PROCESSED)
                    Success(Unit)
                }

                PROCESSED -> Failure(RequestAlreadyProcessed)
                DEAD -> Failure(RequestMarkedAsDead)
            }
        } ?: Failure(PostboxError.RequestNotFound)

    override fun markFailed(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?
    ): Result<Unit, PostboxError> = findRequest(requestId)?.let {
        when (it.status) {
            PENDING -> {
                requests[requestId] = Record(
                    it.processAt + delayReprocessing,
                    it.request,
                    it.response ?: response,
                    failures = it.failures + 1
                )
                Success(Unit)
            }

            PROCESSED -> Failure(RequestAlreadyProcessed)
            DEAD -> Failure(RequestMarkedAsDead)
        }
    } ?: Failure(PostboxError.RequestNotFound)

    override fun markDead(requestId: RequestId, response: Response?): Result<Unit, PostboxError> =
        findRequest(requestId)?.let {
            when (it.status) {
                PENDING -> {
                    requests[requestId] = Record(it.processAt, it.request, it.response ?: response, DEAD)
                    Success(Unit)
                }

                PROCESSED -> Failure(RequestAlreadyProcessed)
                DEAD -> {
                    requests[requestId] = Record(it.processAt, it.request, it.response ?: response, DEAD)
                    Success(Unit)
                }
            }
        } ?: Failure(PostboxError.RequestNotFound)

    override fun status(requestId: RequestId) =
        findRequest(requestId)?.let {
            when (it.status) {
                PENDING -> Success(RequestProcessingStatus.Pending(it.failures, it.processAt))
                PROCESSED -> Success(RequestProcessingStatus.Processed(it.response!!))
                DEAD -> Success(RequestProcessingStatus.Dead(it.response))
            }
        } ?: Failure(PostboxError.RequestNotFound)

    override fun pendingRequests(batchSize: Int, atTime: Instant) = requests
        .filter { it.value.response == null && it.value.status == PENDING && it.value.processAt <= atTime }
        .map { Postbox.PendingRequest(it.key, it.value.request, it.value.processAt) }
        .toList()

    private data class Record(
        val processAt: Instant,
        val request: Request,
        val response: Response? = null,
        val status: Status = PENDING,
        val failures: Int = 0
    )

    private enum class Status {
        PENDING, PROCESSED, DEAD
    }
}
