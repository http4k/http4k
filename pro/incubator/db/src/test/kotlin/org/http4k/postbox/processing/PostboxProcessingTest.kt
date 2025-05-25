package org.http4k.postbox.processing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.FixedTimeSource
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.http4k.db.InMemoryTransactor
import org.http4k.events.StdOutEvents
import org.http4k.postbox.Postbox
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.postbox.processing.PostboxProcessing.Companion.defaultBackoffStrategy
import org.http4k.postbox.storage.inmemory.InMemoryPostbox
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Duration.ofSeconds

class PostboxProcessingTest {

    private val timeSource = FixedTimeSource()
    private val transactor = InMemoryTransactor<Postbox>(InMemoryPostbox(timeSource))
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
    fun `process a single pending request`() = runBlocking {
        val requestId = RequestId.of("0")

        store(requestId, requestForSuccess)
        getProcessor(1).start()

        checkPendingRequest(emptyList())
        checkStatus(requestId, Processed(Response(OK)))
    }

    @Test
    fun `a failed request gets scheduled to be processed later`() = runBlocking {
        val requestId = RequestId.of("0")
        val now = timeSource()

        store(requestId, requestForFailure)
        getProcessor(1).start()

        checkStatus(requestId, Pending(1, now + reprocessingDelay))
    }

    @Test
    fun `a failed request does not affect other requests in batch`() = runBlocking {
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
    fun `a failed request gets marked as dead after maximum attempts reached`() = runBlocking {
        val requestId = RequestId.of("0")

        store(requestId, requestForFailure)
        getProcessor(4).start()

        checkStatus(requestId, RequestProcessingStatus.Dead(Response(BAD_GATEWAY)))
    }

    @Test
    fun `default backoff strategy`() = runBlocking {
        val randomSource: RandomSource = { 7 }
        assertThat(defaultBackoffStrategy(0, randomSource), equalTo(ofSeconds(12)))
        assertThat(defaultBackoffStrategy(1, randomSource), equalTo(ofSeconds(17)))
        assertThat(defaultBackoffStrategy(2, randomSource), equalTo(ofSeconds(27)))
        assertThat(defaultBackoffStrategy(3, randomSource), equalTo(ofSeconds(47)))
        assertThat(defaultBackoffStrategy(4, randomSource), equalTo(ofSeconds(87)))
        assertThat(defaultBackoffStrategy(5, randomSource), equalTo(ofSeconds(167)))
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

