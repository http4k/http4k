/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.peek
import dev.forkhandles.result4k.peekFailure
import dev.forkhandles.time.TimeSource
import dev.forkhandles.time.systemTime
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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ScheduledPostboxProcessing is a background process that polls the Postbox for pending requests and processes them.
 */
class ScheduledPostboxProcessing(
    private val transactor: TransactionalPostbox,
    private val target: HttpHandler,
    private val batchSize: Int = 10,
    private val maxFailures: Int = 3,
    private val maxPollingTime: Duration = Duration.ofSeconds(5),
    private val lease: Duration = Duration.ofSeconds(30),
    private val shutdownGracePeriod: Duration = Duration.ofSeconds(30),
    private val events: Events = { },
    private val scheduler: ScheduledExecutorService = defaultScheduler(),
    private val timeSource: TimeSource = systemTime,
    private val backoffStrategy: BackoffStrategy = ::defaultBackoffStrategy,
    private val successCriteria: (Response) -> Boolean = { it.status.successful }
) {
    private val stopped = AtomicBoolean(false)

    fun start() {
        scheduler.scheduleWithFixedDelay(::poll, 0, maxPollingTime.toMillis(), MILLISECONDS)
    }

    fun stop() {
        if (stopped.getAndSet(true)) return
        if (!scheduler.isShutdown) {
            scheduler.shutdown()
            if (!awaitTermination()) events(ShutdownTimedOut(shutdownGracePeriod))
        }
    }

    private fun awaitTermination() = try {
        scheduler.awaitTermination(shutdownGracePeriod.toMillis(), MILLISECONDS)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        false
    }

    private fun poll() {
        if (stopped.get()) return
        val t0 = timeSource()
        val result = processPendingRequests()
        val elapsedTime = Duration.between(t0, timeSource())

        result
            .peek { events(BatchProcessingSucceeded(it, elapsedTime)) }
            .peekFailure { events(BatchProcessingFailed(it.reason)) }

        events(PollWait(maxPollingTime))
    }

    fun processPendingRequests(): Result<Int, RequestProcessingError> =
        transactor.performAsResult { postbox ->
            val claimed = postbox.claim(batchSize, timeSource(), lease)
            claimed.forEach { pending -> processPendingRequest(postbox, pending) }
            claimed.size
        }.mapFailure { RequestProcessingError(it.message.orEmpty()) }

    private fun processPendingRequest(postbox: Postbox, pending: Postbox.PendingRequest) {
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
}

private fun defaultScheduler() = ScheduledThreadPoolExecutor(1, Thread.ofVirtual().factory())
