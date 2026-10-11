/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.events.Events
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Instant.EPOCH

class ScheduledPostboxProcessingTest : PostboxProcessingContract() {

    private val scheduler = DeterministicScheduler(EPOCH)

    override val timeSource = scheduler::currentTime

    private fun processing(
        target: HttpHandler = testTarget,
        events: Events = {}
    ) = ScheduledPostboxProcessing(
        transactor,
        target,
        pollInterval = pollInterval,
        events = events,
        scheduler = scheduler,
        timeSource = timeSource,
        backoffStrategy = { _, _ -> reprocessingDelay }
    )

    override fun runPolls(polls: Int, target: HttpHandler, events: Events) {
        processing(target, events).start()
        scheduler.runUntilIdle()
        repeat(polls - 1) { scheduler.tick(pollInterval) }
    }

    override fun processPendingRequests(target: HttpHandler, events: Events) {
        processing(target, events).processPendingRequests()
    }

    override fun startWithRealThreads(
        target: HttpHandler,
        pollInterval: Duration,
        shutdownGracePeriod: Duration,
        events: Events
    ): () -> Unit = ScheduledPostboxProcessing(
        transactor,
        target,
        pollInterval = pollInterval,
        shutdownGracePeriod = shutdownGracePeriod,
        events = events,
        timeSource = timeSource
    ).apply { start() }::stop

    @Test
    fun `polls again once the poll interval has elapsed`() {
        processing().start()
        scheduler.runUntilIdle()

        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)

        scheduler.tick(pollInterval - ofMillis(1))
        checkStatus(requestId, Pending(0, EPOCH))

        scheduler.tick(ofMillis(1))
        checkStatus(requestId, Processed(Response(OK)))
    }

    @Test
    fun `does not poll after being stopped`() {
        val processing = processing()
        processing.start()
        scheduler.runUntilIdle()
        processing.stop()

        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        scheduler.tick(pollInterval.multipliedBy(10))

        checkStatus(requestId, Pending(0, EPOCH))
    }
}
