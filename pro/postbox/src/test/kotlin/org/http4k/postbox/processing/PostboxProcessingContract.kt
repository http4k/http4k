/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.TimeSource
import dev.forkhandles.tx.mem.InMemoryTransactor
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.OK
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.postbox.Postbox
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.RequestProcessingStatus.Dead
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.postbox.RequestProcessingStatus.Processing
import org.http4k.postbox.processing.ProcessingEvent.PollWait
import org.http4k.postbox.processing.ProcessingEvent.RequestMarkedDead
import org.http4k.postbox.processing.ProcessingEvent.RequestProcessingFailed
import org.http4k.postbox.processing.ProcessingEvent.RequestProcessingSucceeded
import org.http4k.postbox.processing.ProcessingEvent.RequestScheduledForRetry
import org.http4k.postbox.processing.ProcessingEvent.ShutdownTimedOut
import org.http4k.postbox.processing.RequestProcessingFailureReason.FAILED_TO_MARK_PROCESSED
import org.http4k.postbox.storage.inmemory.InMemoryPostbox
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Duration.ofHours
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

abstract class PostboxProcessingContract {

    protected abstract val timeSource: TimeSource

    protected val postbox by lazy { InMemoryPostbox(timeSource) }
    protected val transactor by lazy { InMemoryTransactor<Postbox, Postbox>(postbox, { postbox }) }
    protected val testTarget = routes(
        "/success" bind { Response(OK) },
        "/failure" bind { Response(BAD_GATEWAY) })

    protected val requestForSuccess = Request(GET, "/success")
    protected val requestForFailure = Request(GET, "/failure")

    protected val reprocessingDelay = ofSeconds(5)
    protected val pollInterval = ofSeconds(5)

    abstract fun runPolls(
        polls: Int,
        target: HttpHandler = testTarget,
        events: Events = {}
    )

    abstract fun processPendingRequests(target: HttpHandler = testTarget, events: Events = {})

    abstract fun startWithRealThreads(
        target: HttpHandler = testTarget,
        pollInterval: Duration = this.pollInterval,
        shutdownGracePeriod: Duration = ofSeconds(5),
        events: Events = {}
    ): () -> Unit

    @Test
    fun `process a single pending request`() {
        val requestId = RequestId.of("0")

        store(requestId, requestForSuccess)
        runPolls(1)

        checkPendingRequest(emptyList())
        checkStatus(requestId, Processed(Response(OK)))
    }

    @Test
    fun `a failed request gets scheduled to be processed later`() {
        val requestId = RequestId.of("0")
        val now = timeSource()

        store(requestId, requestForFailure)
        runPolls(1)

        checkStatus(requestId, Pending(1, now + reprocessingDelay))
    }

    @Test
    fun `a failed request does not affect other requests in batch`() {
        val now = timeSource()
        val r1 = RequestId.of("1")
        val r2 = RequestId.of("2")
        val r3 = RequestId.of("3")

        store(r1, requestForFailure)
        store(r2, requestForSuccess)
        store(r3, requestForSuccess)

        runPolls(1)

        checkStatus(r1, Pending(1, now + reprocessingDelay))
        checkStatus(r2, Processed(Response(OK)))
        checkStatus(r3, Processed(Response(OK)))
    }

    @Test
    fun `a failed request gets marked as dead after maximum attempts reached`() {
        val requestId = RequestId.of("0")

        store(requestId, requestForFailure)
        runPolls(4)

        checkStatus(requestId, Dead(Response(BAD_GATEWAY)))
    }

    @Test
    fun `a request claimed by another processor is not processed`() {
        val requestId = RequestId.of("0")
        val leaseExpiry = timeSource() + ofSeconds(30)
        val received = mutableListOf<Request>()
        store(requestId, requestForSuccess)
        postbox.claim(10, timeSource(), ofSeconds(30))

        runPolls(1, target = { received += it; Response(OK) })

        assertThat(received, equalTo(emptyList()))
        checkStatus(requestId, Processing(0, leaseExpiry))
    }

    @Test
    fun `emits RequestProcessingSucceeded when a request is processed`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val events = mutableListOf<Event>()

        processPendingRequests(events = { events += it })

        assertThat(events, equalTo(listOf<Event>(RequestProcessingSucceeded(requestId))))
    }

    @Test
    fun `emits RequestScheduledForRetry (not a failure event) when a request does not pass success criteria`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForFailure)
        val events = mutableListOf<Event>()

        processPendingRequests(events = { events += it })

        assertThat(events, equalTo(listOf<Event>(RequestScheduledForRetry(requestId, 1, reprocessingDelay))))
    }

    @Test
    fun `emits RequestMarkedDead (not a failure event) when a request exceeds maxFailures`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForFailure)
        val events = mutableListOf<Event>()

        runPolls(4, events = { events += it })

        assertThat(
            events.filterIsInstance<RequestMarkedDead>(),
            equalTo(listOf(RequestMarkedDead(requestId, 4, "did not pass success criteria after exceeding maxFailures of 3")))
        )
        assertThat(events.filterIsInstance<RequestProcessingFailed>(), equalTo(emptyList()))
    }

    @Test
    fun `emits RequestProcessingFailed when a request cannot be finalised as processed`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val events = mutableListOf<Event>()

        processPendingRequests(
            target = {
                postbox.markDead(requestId, Response(BAD_GATEWAY))
                Response(OK)
            },
            events = { events += it })

        assertThat(
            events.single(),
            equalTo(RequestProcessingFailed(requestId, FAILED_TO_MARK_PROCESSED, "storage failed (cause: request already marked as dead)"))
        )
    }

    @Test
    fun `emits PollWait for the poll interval`() {
        val events = mutableListOf<Event>()

        runPolls(1, events = { events += it })

        assertThat(events.filterIsInstance<PollWait>(), equalTo(listOf(PollWait(pollInterval))))
    }

    @Test
    fun `stop waits for an in-flight request to complete`() {
        val events = mutableListOf<Event>()
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val target = BlockingTarget()
        val stop = startWithRealThreads(target, events = { events += it })

        try {
            assertThat(target.started.await(5, SECONDS), equalTo(true))

            val stopped = CountDownLatch(1)
            thread { stop(); stopped.countDown() }
            assertThat(stopped.await(100, MILLISECONDS), equalTo(false))

            target.release.countDown()

            assertThat(stopped.await(5, SECONDS), equalTo(true))
            checkStatus(requestId, Processed(Response(OK)))
            assertThat(events.filterIsInstance<ShutdownTimedOut>(), equalTo(emptyList()))
        } finally {
            target.release.countDown()
            stop()
        }
    }

    @Test
    fun `stop does not wait for the next poll`() {
        val events = mutableListOf<Event>()
        val polled = CountDownLatch(1)
        val stop = startWithRealThreads(
            pollInterval = ofHours(1),
            events = { if (it is PollWait) polled.countDown(); events += it }
        )
        assertThat(polled.await(5, SECONDS), equalTo(true))

        val stopped = CountDownLatch(1)
        thread { stop(); stopped.countDown() }

        assertThat(stopped.await(1, SECONDS), equalTo(true))
        assertThat(events.filterIsInstance<ShutdownTimedOut>(), equalTo(emptyList()))
    }

    @Test
    fun `stop emits ShutdownTimedOut when in-flight work does not finish within the grace period`() {
        val events = mutableListOf<Event>()
        store(RequestId.of("0"), requestForSuccess)
        val target = BlockingTarget()
        val grace = ofMillis(20)
        val stop = startWithRealThreads(target, shutdownGracePeriod = grace, events = { events += it })

        try {
            assertThat(target.started.await(5, SECONDS), equalTo(true))

            stop()

            assertThat(events.filterIsInstance<ShutdownTimedOut>(), equalTo(listOf(ShutdownTimedOut(grace))))
        } finally {
            target.release.countDown()
        }
    }

    private class BlockingTarget : HttpHandler {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        override fun invoke(request: Request): Response {
            started.countDown()
            release.await()
            return Response(OK)
        }
    }

    protected fun checkStatus(requestId: RequestId, expected: RequestProcessingStatus) {
        assertThat(transactor.perform { it.status(requestId) }, equalTo(Success(expected)))
    }

    private fun checkPendingRequest(expected: List<Postbox.PendingRequest>) {
        assertThat(transactor.perform { it.pendingRequests(10, timeSource()) }, equalTo(expected))
    }

    protected fun store(requestId: RequestId, request: Request) {
        transactor.perform { it.store(requestId, request) }
    }
}
