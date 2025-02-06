package org.http4k.postbox.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.FixedTimeSource
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONTINUE
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.postbox.Postbox.PendingRequest
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.PostboxTransactor
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.RequestProcessingStatus.Dead
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.junit.jupiter.api.Test
import java.time.Duration

abstract class PostboxContract {
    val timeSource = FixedTimeSource()
    private val tick = Duration.ofSeconds(1)
    abstract val postbox: PostboxTransactor

    private val requestId = id(1)
    private val request = Request(GET, "/1")

    @Test
    fun `store request`() {
        val now = timeSource()
        store(requestId, request)

        checkStatus(requestId, Success(Pending(0, now)))
        checkPending(PendingRequest(requestId, request, now))
    }

    @Test
    fun `store request is idempotent`() {
        val requestTwo = Request(GET, "/2")

        val firstPending = store(requestId, request)

        store(requestId, requestTwo)

        checkPending(firstPending)
    }

    @Test
    fun `store request does not update previous request`() {
        val requestTwo = Request(GET, "/2")

        store(requestId, request)

        markProcessed(requestId, Response(I_M_A_TEAPOT), Success(Unit))

        store(requestId, requestTwo, Success(Processed(Response(I_M_A_TEAPOT))))

        checkPending()
    }

    @Test
    fun `store request multiple times does not affect pending retrieval order`() {
        val requestId2 = id(2)
        val request2 = Request(GET, "/2")

        val pendingOne = store(requestId, request)

        timeSource.tick(tick)

        val pendingTwo = store(requestId2, request2)

        checkPending(pendingOne, pendingTwo)

        store(requestId, request, Success(Pending(0, pendingOne.processingTime)))

        checkPending(pendingOne, pendingTwo)
    }

    @Test
    fun `mark request as processed`() {
        store(requestId, request)

        markProcessed(requestId, Response(I_M_A_TEAPOT))

        checkPending()
    }

    @Test
    fun `cannot mark a request as processed if it does not exist`() {
        markProcessed(requestId, Response(I_M_A_TEAPOT), Failure(RequestNotFound))
    }

    @Test
    fun `cannot mark request as processed after it has already been processed`() {
        store(requestId, request)

        markProcessed(requestId, Response(CONTINUE))
        markProcessed(requestId, Response(I_M_A_TEAPOT), expects = Failure(RequestAlreadyProcessed))

        checkPending()
        checkStatus(requestId, Success(Processed(Response(CONTINUE))))
    }

    @Test
    fun `cannot mark request as processed after it has been marked as failed`() {
        store(requestId, request)

        markDead(requestId, Response(BAD_REQUEST))
        markProcessed(requestId, Response(I_M_A_TEAPOT), Failure(RequestMarkedAsDead))

        checkPending()
    }

    @Test
    fun `mark request as dead`() {
        store(requestId, request)

        markDead(requestId, Response(I_M_A_TEAPOT))

        checkPending()
        checkStatus(requestId, Success(Dead(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `cannot mark a request as dead if it does not exist`() {
        markDead(requestId, Response(I_M_A_TEAPOT), Failure(RequestNotFound))
    }

    @Test
    fun `mark request as dead without a response`() {
        store(requestId, request)

        markDead(requestId)

        checkPending()
        checkStatus(requestId, Success(Dead()))
    }

    @Test
    fun `marks request as dead multiple times does not update an existing response`() {
        store(requestId, request)

        markDead(requestId, Response(I_M_A_TEAPOT))
        markDead(requestId, Response(BAD_REQUEST))

        checkPending()
        checkStatus(requestId, Success(Dead(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `marks request as dead multiple times store response if previous was null`() {
        store(requestId, request)

        markDead(requestId, null)
        markDead(requestId, Response(BAD_REQUEST))

        checkPending()
        checkStatus(requestId, Success(Dead(Response(BAD_REQUEST))))
    }

    @Test
    fun `cannot mark request as dead after it has been processed`() {
        store(requestId, request)

        markProcessed(requestId, Response(I_M_A_TEAPOT))
        markDead(requestId, Response(BAD_REQUEST), Failure(RequestAlreadyProcessed))

        checkPending()
    }

    @Test
    fun `check status of a non existing request`() {
        checkStatus(requestId, Failure(RequestNotFound))
    }

    @Test
    fun `check status of a pending request`() {
        val now = timeSource()
        store(requestId, request)

        checkStatus(requestId, Success(Pending(0, now)))
    }

    @Test
    fun `check status of a processed request`() {
        store(requestId, request)
        markProcessed(requestId, Response(I_M_A_TEAPOT), Success(Unit))

        checkStatus(requestId, Success(Processed(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `check status of a dead request`() {
        val now = timeSource()
        store(requestId, request)

        checkStatus(requestId, Success(Pending(0, now)))

        markDead(requestId, Response(I_M_A_TEAPOT), Success(Unit))

        checkStatus(requestId, Success(Dead(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `mark request as failed`() {
        val now = timeSource()
        val later = Duration.ofSeconds(10)

        store(requestId, request)

        markFailed(requestId, later, Response(I_M_A_TEAPOT))

        checkPending()

        checkStatus(requestId, Success(Pending(1, now + later)))
    }

    @Test
    fun `mark request as failed multiple times increases counter and processing time`() {
        val now = timeSource()
        val later = Duration.ofSeconds(10)

        store(requestId, request)

        markFailed(requestId, later, Response(I_M_A_TEAPOT))
        markFailed(requestId, later, Response(I_M_A_TEAPOT))
        markFailed(requestId, later, Response(I_M_A_TEAPOT))

        checkPending()

        checkStatus(requestId, Success(Pending(3, now + Duration.ofSeconds(30))))
    }

    private fun markFailed(
        requestId: RequestId,
        delay: Duration,
        response: Response? = null,
        expecting: Result<Unit, PostboxError> = Success(Unit)
    ) {
        val result = postbox.perform { it.markFailed(requestId, delay, response) }
        assertThat(result, equalTo(expecting))
    }

    private fun markDead(
        requestId: RequestId, response: Response? = null,
        expecting: Result<Unit, PostboxError> = Success(Unit)
    ) {
        val result = postbox.perform { it.markDead(requestId, response) }
        assertThat(result, equalTo(expecting))
    }

    private fun checkStatus(requestId: RequestId, expected: Result<RequestProcessingStatus, PostboxError>) {
        val status = postbox.perform { it.status(requestId) }
        assertThat(status, equalTo(expected))
    }

    private fun markProcessed(
        requestId: RequestId,
        response: Response,
        expects: Result<Unit, PostboxError> = Success(Unit)
    ) {
        val result = postbox.perform { it.markProcessed(requestId, response) }
        assertThat(result, equalTo(expects))
    }

    private fun checkPending(vararg expected: PendingRequest) {
        val pending = postbox.perform { it.pendingRequests(10, timeSource()) }
        assertThat(pending, equalTo(expected.toList()))
    }

    private fun store(
        requestId: RequestId,
        request: Request,
        expectedStatus: Result<RequestProcessingStatus, PostboxError> = Success(Pending(0, timeSource()))
    ): PendingRequest {
        val result = postbox.perform { it.store(requestId, request) }
        assertThat(result, equalTo(expectedStatus))
        return PendingRequest(requestId, request, timeSource())
    }

    private fun id(id: Int) = RequestId.of(id.toString())

}

