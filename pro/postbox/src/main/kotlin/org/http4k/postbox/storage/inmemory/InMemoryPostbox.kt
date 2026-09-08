/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.inmemory

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.TimeSource
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.storage.inmemory.InMemoryPostbox.Status.DEAD
import org.http4k.postbox.storage.inmemory.InMemoryPostbox.Status.PENDING
import org.http4k.postbox.storage.inmemory.InMemoryPostbox.Status.PROCESSED
import org.http4k.postbox.storage.inmemory.InMemoryPostbox.Status.PROCESSING
import java.time.Duration
import java.time.Instant

class InMemoryPostbox(val timeSource: TimeSource) : Postbox {
    private val requests = mutableMapOf<RequestId, Record>()
    private val lock = Any()

    private fun findRequest(requestId: RequestId) = requests[requestId]

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> = synchronized(lock) {
        val now = timeSource()
        val existingRequest = findRequest(requestId)
        if (existingRequest == null) {
            requests[requestId] = Record(now, request)
            Success(RequestProcessingStatus.Pending(0, now))
        } else {
            when (existingRequest.status) {
                PENDING -> Success(
                    RequestProcessingStatus.Pending(existingRequest.failures, existingRequest.processAt)
                )

                PROCESSING -> Success(
                    RequestProcessingStatus.Processing(existingRequest.failures, existingRequest.processAt)
                )

                PROCESSED -> Success(RequestProcessingStatus.Processed(existingRequest.response!!))

                DEAD -> Success(RequestProcessingStatus.Dead(existingRequest.response))
            }
        }
    }

    override fun claim(batchSize: Int, atTime: Instant, lease: Duration): List<Postbox.PendingRequest> = synchronized(lock) {
        reclaimExpired(atTime)
        duePending(atTime, batchSize).map { pending ->
            requests[pending.requestId] = Record(atTime + lease, pending.record.request, pending.record.response, PROCESSING, pending.record.failures)
            Postbox.PendingRequest(pending.requestId, pending.record.request, atTime + lease, pending.record.failures)
        }
    }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> = synchronized(lock) {
        findRequest(requestId)?.let {
            when (it.status) {
                PENDING, PROCESSING -> {
                    requests[requestId] = Record(it.processAt, it.request, response, PROCESSED)
                    Success(Unit)
                }

                PROCESSED -> Failure(RequestAlreadyProcessed)

                DEAD -> Failure(RequestMarkedAsDead)
            }
        } ?: Failure(PostboxError.RequestNotFound)
    }

    override fun markFailed(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?
    ): Result<Unit, PostboxError> = synchronized(lock) {
        findRequest(requestId)?.let {
            when (it.status) {
                PENDING -> {
                    requests[requestId] = Record(
                        it.processAt + delayReprocessing,
                        it.request,
                        response,
                        failures = it.failures + 1
                    )
                    Success(Unit)
                }

                PROCESSING -> {
                    requests[requestId] = Record(
                        timeSource() + delayReprocessing,
                        it.request,
                        response,
                        failures = it.failures + 1
                    )
                    Success(Unit)
                }

                PROCESSED -> Failure(RequestAlreadyProcessed)

                DEAD -> Failure(RequestMarkedAsDead)
            }
        } ?: Failure(PostboxError.RequestNotFound)
    }

    override fun markDead(requestId: RequestId, response: Response?): Result<Unit, PostboxError> = synchronized(lock) {
        findRequest(requestId)?.let {
            when (it.status) {
                PENDING, PROCESSING -> {
                    requests[requestId] = Record(it.processAt, it.request, response, DEAD)
                    Success(Unit)
                }

                PROCESSED -> Failure(RequestAlreadyProcessed)

                DEAD -> {
                    requests[requestId] = Record(it.processAt, it.request, it.response ?: response, DEAD)
                    Success(Unit)
                }
            }
        } ?: Failure(PostboxError.RequestNotFound)
    }

    override fun status(requestId: RequestId) =
        findRequest(requestId)?.let {
            when (it.status) {
                PENDING -> Success(RequestProcessingStatus.Pending(it.failures, it.processAt))
                PROCESSING -> Success(RequestProcessingStatus.Processing(it.failures, it.processAt))
                PROCESSED -> Success(RequestProcessingStatus.Processed(it.response!!))
                DEAD -> Success(RequestProcessingStatus.Dead(it.response))
            }
        } ?: Failure(PostboxError.RequestNotFound)

    override fun pendingRequests(batchSize: Int, atTime: Instant): List<Postbox.PendingRequest> =
        duePending(atTime, batchSize).map {
            Postbox.PendingRequest(it.requestId, it.record.request, it.record.processAt, it.record.failures)
        }

    private fun duePending(atTime: Instant, batchSize: Int): List<PendingEntry> = requests
        .filter { it.value.status == PENDING && it.value.processAt <= atTime }
        .map { PendingEntry(it.key, it.value) }
        .sortedWith(compareBy<PendingEntry> { it.record.processAt }.thenBy { it.requestId.value })
        .take(batchSize)

    private fun reclaimExpired(atTime: Instant) {
        requests.entries
            .filter { it.value.status == PROCESSING && it.value.processAt <= atTime }
            .forEach { (requestId, record) ->
                requests[requestId] = record.copy(status = PENDING)
            }
    }

    private data class PendingEntry(val requestId: RequestId, val record: Record)

    private data class Record(
        val processAt: Instant,
        val request: Request,
        val response: Response? = null,
        val status: Status = PENDING,
        val failures: Int = 0
    )

    private enum class Status {
        PENDING, PROCESSING, PROCESSED, DEAD
    }
}
