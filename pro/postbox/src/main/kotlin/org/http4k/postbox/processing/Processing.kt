/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.peek
import dev.forkhandles.result4k.peekFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.events.Events
import org.http4k.postbox.Postbox
import org.http4k.postbox.TransactionalPostbox
import org.http4k.postbox.performAsResult
import org.http4k.postbox.processing.ProcessingEvent.BatchProcessingFailed
import org.http4k.postbox.processing.ProcessingEvent.BatchProcessingSucceeded
import org.http4k.postbox.processing.ProcessingEvent.PollWait
import org.http4k.postbox.processing.ProcessingEvent.RequestMarkedDead
import org.http4k.postbox.processing.ProcessingEvent.RequestProcessingFailed
import org.http4k.postbox.processing.ProcessingEvent.RequestProcessingSucceeded
import org.http4k.postbox.processing.ProcessingEvent.RequestScheduledForRetry
import org.http4k.postbox.processing.ProcessingEvent.ShutdownTimedOut
import org.http4k.postbox.processing.RequestProcessingFailureReason.FAILED_TO_MARK_DEAD
import org.http4k.postbox.processing.RequestProcessingFailureReason.FAILED_TO_MARK_PROCESSED
import org.http4k.postbox.processing.RequestProcessingFailureReason.FAILED_TO_SCHEDULE_RETRY
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

/**
 * PostboxProcessing is a background process that polls the Postbox for pending requests and processes them.
 */
class PostboxProcessing(
    private val transactor: TransactionalPostbox,
    private val target: HttpHandler,
    private val batchSize: Int = 10,
    private val maxFailures: Int = 3,
    private val maxPollingTime: Duration = Duration.ofSeconds(5),
    private val lease: Duration = Duration.ofSeconds(30),
    private val shutdownGracePeriod: Duration = Duration.ofSeconds(30),
    private val events: Events = { },
    private val context: ExecutionContext = DefaultExecutionContext(shutdownGracePeriod),
    private val backoffStrategy: BackoffStrategy = ::defaultBackoffStrategy,
    private val successCriteria: (Response) -> Boolean = { it.status.successful }
) {
    private val stopped = AtomicBoolean(false)

    private val task = Runnable {
        while (context.isRunning()) {
            val t0 = context.currentTime()
            val result = processPendingRequests(successCriteria)
            val elapsedTime = Duration.between(t0, context.currentTime())

            result
                .peek { events(BatchProcessingSucceeded(it, elapsedTime)) }
                .peekFailure { events(BatchProcessingFailed(it.reason)) }

            val remainingTime = maxPollingTime - elapsedTime
            if (remainingTime > Duration.ZERO) {
                events(PollWait(remainingTime))
                context.pause(remainingTime)
            }
        }
    }

    fun stop() {
        if (!stopped.getAndSet(true)) {
            if (!context.stop()) {
                events(ShutdownTimedOut(shutdownGracePeriod))
            }
        }
    }

    fun start() {
        context.start(task)
    }

    fun processPendingRequests(successCriteria: (Response) -> Boolean): Result<Int, RequestProcessingError> =
        transactor.performAsResult { postbox ->
            val claimed = postbox.claim(batchSize, context.currentTime(), lease)
            claimed.forEach { pending -> processPendingRequest(postbox, pending, successCriteria) }
            claimed.size
        }.mapFailure { RequestProcessingError(it.message.orEmpty()) }

    private fun processPendingRequest(
        postbox: Postbox, pending: Postbox.PendingRequest,
        successCriteria: (Response) -> Boolean
    ) {
        val response = target(pending.request)
        when {
            successCriteria(response) -> finaliseProcessed(postbox, pending, response)
            pending.failures >= maxFailures -> finaliseDead(postbox, pending, response)
            else -> finaliseForRetry(postbox, pending, response)
        }
    }

    private fun finaliseProcessed(postbox: Postbox, pending: Postbox.PendingRequest, response: Response) {
        postbox.markProcessed(pending.requestId, response)
            .peek { events(RequestProcessingSucceeded(pending.requestId)) }
            .peekFailure {
                events(RequestProcessingFailed(pending.requestId, FAILED_TO_MARK_PROCESSED, it.description))
            }
    }

    private fun finaliseDead(postbox: Postbox, pending: Postbox.PendingRequest, response: Response) {
        postbox.markDead(pending.requestId, response)
            .peek {
                events(
                    RequestMarkedDead(
                        pending.requestId,
                        pending.failures + 1,
                        "did not pass success criteria after exceeding maxFailures of $maxFailures"
                    )
                )
            }
            .peekFailure {
                events(RequestProcessingFailed(pending.requestId, FAILED_TO_MARK_DEAD, it.description))
            }
    }

    private fun finaliseForRetry(postbox: Postbox, pending: Postbox.PendingRequest, response: Response) {
        val delay = backoffStrategy(pending.failures, { (0..it).random() })
        postbox.markFailed(pending.requestId, delay, response)
            .peek { events(RequestScheduledForRetry(pending.requestId, pending.failures + 1, delay)) }
            .peekFailure {
                events(RequestProcessingFailed(pending.requestId, FAILED_TO_SCHEDULE_RETRY, it.description))
            }
    }

    companion object {
        fun defaultBackoffStrategy(failures: Int, random: RandomSource): Duration = Duration.ofMillis(
            (2.0.pow(failures.toDouble()) * Duration.ofSeconds(5).toMillis() + random(10) * 1000).toLong()
        )
    }
}

data class RequestProcessingError(val reason: String)

typealias RandomSource = (Int) -> Int

typealias BackoffStrategy = (failures: Int, random: RandomSource) -> Duration
