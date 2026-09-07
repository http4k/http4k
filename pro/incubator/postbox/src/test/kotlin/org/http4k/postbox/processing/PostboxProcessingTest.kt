/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.FixedTimeSource
import dev.forkhandles.tx.mem.InMemoryTransactor
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.http4k.events.Event
import org.http4k.events.StdOutEvents
import org.http4k.postbox.Postbox
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.postbox.processing.PostboxProcessing.Companion.defaultBackoffStrategy
import org.http4k.postbox.processing.ProcessingEvent.RequestMarkedDead
import org.http4k.postbox.processing.ProcessingEvent.RequestProcessingFailed
import org.http4k.postbox.processing.ProcessingEvent.RequestProcessingSucceeded
import org.http4k.postbox.processing.ProcessingEvent.RequestScheduledForRetry
import org.http4k.postbox.processing.ProcessingEvent.ShutdownTimedOut
import org.http4k.postbox.storage.inmemory.InMemoryPostbox
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Duration.ofSeconds

class PostboxProcessingTest {

    private val timeSource = FixedTimeSource()
    private val postbox = InMemoryPostbox(timeSource)
    private val transactor = InMemoryTransactor<Postbox, Postbox>(postbox, { postbox })
    private val testTarget = routes("/success" bind { Response(OK) },
        "/failure" bind { Response(BAD_GATEWAY) },
        "/permanent_failure" bind { Response(UNPROCESSABLE_ENTITY) },
        "/exception" bind { throw RuntimeException("boom") })

    private val requestForSuccess = Request(GET, "/success")
    private val requestForFailure = Request(GET, "/failure")

    private val reprocessingDelay = ofSeconds(5)

    private fun getProcessor(iterations: Int) = PostboxProcessing(transactor,
        testTarget,
        context = TestExecutionContext(timeSource, iterations),
        events = StdOutEvents,
        backoffStrategy = { _, _ -> reprocessingDelay })

    @Test
    fun `process a single pending request`() {
        val requestId = RequestId.of("0")

        store(requestId, requestForSuccess)
        getProcessor(1).start()

        checkPendingRequest(emptyList())
        checkStatus(requestId, Processed(Response(OK)))
    }

    @Test
    fun `a failed request gets scheduled to be processed later`() {
        val requestId = RequestId.of("0")
        val now = timeSource()

        store(requestId, requestForFailure)
        getProcessor(1).start()

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

        getProcessor(1).start()

        checkPendingRequest(listOf(Postbox.PendingRequest(r1, requestForFailure, now + reprocessingDelay, 1)))
        checkStatus(r2, Processed(Response(OK)))
        checkStatus(r3, Processed(Response(OK)))
    }

    @Test
    fun `a failed request gets marked as dead after maximum attempts reached`() {
        val requestId = RequestId.of("0")

        store(requestId, requestForFailure)
        getProcessor(4).start()

        checkStatus(requestId, RequestProcessingStatus.Dead(Response(BAD_GATEWAY)))
    }

    @Test
    fun `a request claimed by another processor is not re-processed`() {
        val requestId = RequestId.of("0")

        store(requestId, requestForSuccess)

        val lease = ofSeconds(30)
        postbox.claim(10, timeSource(), lease)

        transactor.perform { it.markProcessed(requestId, Response(OK)) }

        checkPendingRequest(emptyList())
        checkStatus(requestId, Processed(Response(OK)))
    }

    @Test
    fun `a request is reclaimed and reprocessed after its lease expires`() {
        val requestId = RequestId.of("0")
        val lease = ofSeconds(30)

        store(requestId, requestForFailure)

        postbox.claim(10, timeSource(), lease)

        timeSource.tick(ofSeconds(31))

        val reclaimed = postbox.claim(10, timeSource(), lease)

        assertThat(reclaimed, equalTo(listOf(Postbox.PendingRequest(requestId, requestForFailure, timeSource() + lease, 0))))
    }

    @Test
    fun `default backoff strategy`() {
        val randomSource: RandomSource = { 7 }
        assertThat(defaultBackoffStrategy(0, randomSource), equalTo(ofSeconds(12)))
        assertThat(defaultBackoffStrategy(1, randomSource), equalTo(ofSeconds(17)))
        assertThat(defaultBackoffStrategy(2, randomSource), equalTo(ofSeconds(27)))
        assertThat(defaultBackoffStrategy(3, randomSource), equalTo(ofSeconds(47)))
        assertThat(defaultBackoffStrategy(4, randomSource), equalTo(ofSeconds(87)))
        assertThat(defaultBackoffStrategy(5, randomSource), equalTo(ofSeconds(167)))
    }

    @Test
    fun `emits RequestProcessingSucceeded when a request is processed`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val events = mutableListOf<Event>()

        processOnce(requestId, events)

        assertThat(
            events,
            equalTo(listOf<ProcessingEvent>(RequestProcessingSucceeded(requestId)))
        )
    }

    @Test
    fun `emits RequestScheduledForRetry (not a failure event) when a request does not pass success criteria`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForFailure)
        val events = mutableListOf<Event>()

        processOnce(requestId, events)

        assertThat(events, equalTo(listOf<ProcessingEvent>(RequestScheduledForRetry(requestId, 1, reprocessingDelay))))
    }

    @Test
    fun `emits RequestMarkedDead (not a failure event) when a request exceeds maxFailures`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForFailure)
        val events = mutableListOf<Event>()
        val processor = PostboxProcessing(transactor,
            testTarget,
            context = TestExecutionContext(timeSource, 4),
            events = { events += it },
            maxFailures = 3,
            backoffStrategy = { _, _ -> reprocessingDelay })

        processor.start()

        assertThat(
            events.filterIsInstance<RequestMarkedDead>(),
            equalTo(listOf(RequestMarkedDead(requestId, 4, "did not pass success criteria after exceeding maxFailures of 3")))
        )
        assertThat(events.filterIsInstance<RequestProcessingFailed>(), equalTo(emptyList<RequestProcessingFailed>()))
    }

    @Test
    fun `emits RequestProcessingFailed when a request cannot be finalised as processed`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val events = mutableListOf<Event>()
        val processor = PostboxProcessing(transactor,
            { request ->
                postbox.markDead(requestId, Response(BAD_GATEWAY))
                Response(OK)
            },
            events = { events += it },
            backoffStrategy = { _, _ -> reprocessingDelay })

        processor.processPendingRequests { it.status.successful }

        assertThat(events.single(), equalTo(RequestProcessingFailed(requestId, RequestProcessingFailureReason.FAILED_TO_MARK_PROCESSED, "storage failed (cause: request already marked as dead)")))
    }

    private fun processOnce(requestId: RequestId, events: MutableList<Event>) {
        val processor = PostboxProcessing(transactor,
            testTarget,
            context = TestExecutionContext(timeSource, 1),
            events = { events += it },
            backoffStrategy = { _, _ -> reprocessingDelay })
        processor.processPendingRequests { it.status.successful }
    }

    @Test
    fun `stop emits ShutdownTimedOut when in-flight work does not finish within the grace period`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val events = mutableListOf<Event>()
        val grace = ofSeconds(10)
        val context = SimulatedExecutionContext(timeSource, grace).apply {
            thread.busyUntil = timeSource() + ofSeconds(60)
        }

        PostboxProcessing(transactor,
            testTarget,
            shutdownGracePeriod = grace,
            context = context,
            events = { events += it })
            .apply { start(); stop() }

        assertThat(events, equalTo(listOf<Event>(ShutdownTimedOut(grace))))
    }

    @Test
    fun `stop does not emit ShutdownTimedOut when in-flight work finishes within the grace period`() {
        val requestId = RequestId.of("0")
        store(requestId, requestForSuccess)
        val events = mutableListOf<Event>()
        val grace = ofSeconds(10)
        val context = SimulatedExecutionContext(timeSource, grace).apply {
            thread.busyUntil = timeSource() + ofSeconds(5)
        }

        PostboxProcessing(transactor,
            testTarget,
            shutdownGracePeriod = grace,
            context = context,
            events = { events += it })
            .apply { start(); stop() }

        assertThat(events, equalTo(emptyList<Event>()))
    }

    @Test
    fun `stop simulates waiting for in-flight work up to the grace period using the time source`() {
        val now = timeSource()
        val grace = ofSeconds(10)
        val context = SimulatedExecutionContext(timeSource, grace).apply {
            thread.busyUntil = timeSource() + ofSeconds(60)
        }

        PostboxProcessing(transactor, testTarget, shutdownGracePeriod = grace, context = context).apply { stop() }

        assertThat(context.finished, equalTo(false))
        assertThat(timeSource(), equalTo(now + grace))
    }

    @Test
    fun `stop simulates waiting only until in-flight work completes when within the grace period`() {
        val now = timeSource()
        val grace = ofSeconds(10)
        val context = SimulatedExecutionContext(timeSource, grace).apply {
            thread.busyUntil = timeSource() + ofSeconds(5)
        }

        PostboxProcessing(transactor, testTarget, shutdownGracePeriod = grace, context = context).apply { stop() }

        assertThat(context.finished, equalTo(true))
        assertThat(timeSource(), equalTo(now + ofSeconds(5)))
    }

    private fun checkStatus(requestId: RequestId, processed: RequestProcessingStatus) {
        assertThat(
            transactor.perform { it.status(requestId) }, equalTo(Success(processed))
        )
    }

    private fun checkPendingRequest(expected: List<Postbox.PendingRequest>) {
        assertThat(transactor.perform { it.pendingRequests(10, timeSource()) }, equalTo(expected))
    }

    private fun store(requestId: RequestId, request: Request) {
        transactor.perform { it.store(requestId, request) }
    }
}
