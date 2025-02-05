package org.http4k.postbox

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
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestProcessingStatus.Dead
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.junit.jupiter.api.Test
import java.time.Duration

abstract class PostboxContract {
    val timeSource = FixedTimeSource()
    abstract val postbox: PostboxTransactor

    @Test
    fun `store request`() {
        val newRequest = PendingRequest(id(1), Request(GET, "/"))

        store(newRequest)

        checkStatus(newRequest.requestId, Success(Pending))
        checkPending(newRequest)
    }

    @Test
    fun `store request is idempotent`() {
        val requestId = id(1)
        val request1 = Request(GET, "/foo")
        val request2 = Request(GET, "/bar")

        store(PendingRequest(requestId, request1))
        store(PendingRequest(requestId, request2))

        checkPending(PendingRequest(requestId, request1))
    }

    @Test
    fun `store request does not update previous request`() {
        val requestId = id(1)
        val request1 = Request(GET, "/foo")
        val request2 = Request(GET, "/bar")

        store(PendingRequest(requestId, request1))

        markProcessed(requestId, Response(I_M_A_TEAPOT), Success(Unit))

        store(PendingRequest(requestId, request2), expectedStatus = Success(Processed(Response(I_M_A_TEAPOT))))

        checkPending()
    }

    @Test
    fun `store request multiple times does not affect pending retrieval order`() {
        val newRequestOne = PendingRequest(id(1), Request(GET, "/one"))
        val newRequestTwo = PendingRequest(id(2), Request(GET, "/two"))

        store(newRequestOne)
        store(newRequestTwo)

        checkPending(newRequestOne, newRequestTwo)

        store(newRequestOne)

        checkPending(newRequestOne, newRequestTwo)
    }

    @Test
    fun `mark request as processed`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markProcessed(request.requestId, Response(I_M_A_TEAPOT))

        checkPending()
    }

    @Test
    fun `cannot mark a request as processed if it does not exist`() {
        markProcessed(id(1), Response(I_M_A_TEAPOT), Failure(RequestNotFound))
    }

    @Test
    fun `cannot mark request as processed after it has already been processed`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markProcessed(request.requestId, Response(CONTINUE))
        markProcessed(request.requestId, Response(I_M_A_TEAPOT), expects = Failure(RequestAlreadyProcessed))

        checkPending()
        checkStatus(request.requestId, Success(Processed(Response(CONTINUE))))
    }

    @Test
    fun `cannot mark request as processed after it has been marked as failed`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markDead(request.requestId, Response(BAD_REQUEST))
        markProcessed(request.requestId, Response(I_M_A_TEAPOT), Failure(RequestMarkedAsDead))

        checkPending()
    }

    @Test
    fun `mark request as dead`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markDead(requestId = request.requestId, Response(I_M_A_TEAPOT))

        checkPending()
        checkStatus(request.requestId, Success(Dead(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `cannot mark a request as dead if it does not exist`() {
        markDead(id(1), Response(I_M_A_TEAPOT), Failure(RequestNotFound))
    }

    @Test
    fun `mark request as dead without a response`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markDead(requestId = request.requestId)

        checkPending()
        checkStatus(request.requestId, Success(Dead()))
    }

    @Test
    fun `marks request as dead multiple times does not update an existing response`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markDead(requestId = request.requestId, Response(I_M_A_TEAPOT))
        markDead(requestId = request.requestId, Response(BAD_REQUEST))

        checkPending()
        checkStatus(request.requestId, Success(Dead(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `marks request as dead multiple times store response if previous was null`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markDead(requestId = request.requestId, null)
        markDead(requestId = request.requestId, Response(BAD_REQUEST))

        checkPending()
        checkStatus(request.requestId, Success(Dead(Response(BAD_REQUEST))))
    }

    @Test
    fun `cannot mark request as dead after it has been processed`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markProcessed(request.requestId, Response(I_M_A_TEAPOT))
        markDead(request.requestId, Response(BAD_REQUEST), Failure(RequestAlreadyProcessed))

        checkPending()
    }

    @Test
    fun `check status of a request`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        checkStatus(request.requestId, Failure(RequestNotFound))

        store(request)

        checkStatus(request.requestId, Success(Pending))

        markProcessed(request.requestId, Response(I_M_A_TEAPOT), Success(Unit))

        checkStatus(request.requestId, Success(Processed(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `mark request as failed`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markFailed(request.requestId, Duration.ofSeconds(10), Response(I_M_A_TEAPOT))

        checkPending()

        checkStatus(request.requestId, Success(Pending))
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
        request: PendingRequest,
        expectedStatus: Result<RequestProcessingStatus, PostboxError> = Success(Pending)
    ) {
        timeSource.tick(Duration.ofSeconds(1))
        val result = postbox.perform { it.store(request) }
        assertThat(result, equalTo(expectedStatus))
    }

    private fun id(id: Int) = RequestId.of(id.toString())

}

