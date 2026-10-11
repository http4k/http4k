/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.postbox.processing.ProcessingEvent.ShutdownTimedOut
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.time.Instant.EPOCH
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

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
        externalScheduler = scheduler,
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

    @Test
    fun `stop leaves an external scheduler running`() {
        val external = ScheduledThreadPoolExecutor(1)
        val events = mutableListOf<Event>()

        try {
            val processing = ScheduledPostboxProcessing(
                transactor,
                testTarget,
                shutdownGracePeriod = ofSeconds(1),
                events = { events += it },
                externalScheduler = external,
                timeSource = timeSource
            )
            processing.start()
            processing.stop()

            assertThat(external.isShutdown, equalTo(false))
            assertThat(events.filterIsInstance<ShutdownTimedOut>(), equalTo(emptyList()))
        } finally {
            external.shutdownNow()
        }
    }

    @Test
    fun `stop abandons the rest of an in-flight batch`() {
        store(RequestId.of("0"), requestForSuccess)
        store(RequestId.of("1"), requestForSuccess)
        val target = BlockingTarget()
        val stop = startWithRealThreads(target)

        try {
            assertThat(target.started.await(5, SECONDS), equalTo(true))

            val stopped = CountDownLatch(1)
            thread { stop(); stopped.countDown() }
            assertThat(stopped.await(100, MILLISECONDS), equalTo(false))

            target.release.countDown()

            assertThat(stopped.await(5, SECONDS), equalTo(true))
            assertThat(target.calls.get(), equalTo(1))
        } finally {
            target.release.countDown()
            stop()
        }
    }
}
