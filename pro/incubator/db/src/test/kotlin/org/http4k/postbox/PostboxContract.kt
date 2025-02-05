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

    private fun PostboxRequest(requestId: RequestId, request: Request) = requestId to request

    private val Pair<RequestId, Request>.requestId get() = first
    private val Pair<RequestId, Request>.request get() = second
    private fun Pair<RequestId, Request>.toPending()  = PendingRequest(first, second, timeSource())

    @Test
    fun `store request`() {
        val newRequest = PostboxRequest(id(1), Request(GET, "/"))

        store(newRequest.requestId, newRequest.request)

        checkStatus(newRequest.requestId, Success(Pending))
        checkPending(newRequest.toPending())
    }

    @Test
    fun `store request is idempotent`() {
        val requestId = id(1)
        val request1 = Request(GET, "/foo")
        val request2 = Request(GET, "/bar")

        val requestToRemove = PostboxRequest(requestId, request1)
        store(requestToRemove.requestId, requestToRemove.request)
        val firstTime= timeSource()

        val requestToRemove1 = PostboxRequest(requestId, request2)
        store(requestToRemove1.requestId, requestToRemove1.request)

        checkPending(PendingRequest(requestId, request1, firstTime))
    }

    @Test
    fun `store request does not update previous request`() {
        val requestId = id(1)
        val request1 = Request(GET, "/foo")
        val request2 = Request(GET, "/bar")

        val requestToRemove = PostboxRequest(requestId, request1)
        store(requestToRemove.requestId, requestToRemove.request)

        markProcessed(requestId, Response(I_M_A_TEAPOT), Success(Unit))

        val requestToRemove1 = PostboxRequest(requestId, request2)
        store(
            requestToRemove1.requestId,
            requestToRemove1.request,
            expectedStatus = Success(Processed(Response(I_M_A_TEAPOT)))
        )

        checkPending()
    }

    @Test
    fun `store request multiple times does not affect pending retrieval order`() {
        val newRequestOne = PostboxRequest(id(1), Request(GET, "/one"))
        val newRequestTwo = PostboxRequest(id(2), Request(GET, "/two"))

        val pendingOne = store(newRequestOne.requestId, newRequestOne.request)
        val pendingTwo = store(newRequestTwo.requestId, newRequestTwo.request)

        checkPending(pendingOne, pendingTwo)

        store(newRequestOne.requestId, newRequestOne.request)

        checkPending(pendingOne, pendingTwo)
    }

    @Test
    fun `mark request as processed`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markProcessed(request.requestId, Response(I_M_A_TEAPOT))

        checkPending()
    }

    @Test
    fun `cannot mark a request as processed if it does not exist`() {
        markProcessed(id(1), Response(I_M_A_TEAPOT), Failure(RequestNotFound))
    }

    @Test
    fun `cannot mark request as processed after it has already been processed`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markProcessed(request.requestId, Response(CONTINUE))
        markProcessed(request.requestId, Response(I_M_A_TEAPOT), expects = Failure(RequestAlreadyProcessed))

        checkPending()
        checkStatus(request.requestId, Success(Processed(Response(CONTINUE))))
    }

    @Test
    fun `cannot mark request as processed after it has been marked as failed`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markDead(request.requestId, Response(BAD_REQUEST))
        markProcessed(request.requestId, Response(I_M_A_TEAPOT), Failure(RequestMarkedAsDead))

        checkPending()
    }

    @Test
    fun `mark request as dead`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

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
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markDead(requestId = request.requestId)

        checkPending()
        checkStatus(request.requestId, Success(Dead()))
    }

    @Test
    fun `marks request as dead multiple times does not update an existing response`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markDead(requestId = request.requestId, Response(I_M_A_TEAPOT))
        markDead(requestId = request.requestId, Response(BAD_REQUEST))

        checkPending()
        checkStatus(request.requestId, Success(Dead(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `marks request as dead multiple times store response if previous was null`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markDead(requestId = request.requestId, null)
        markDead(requestId = request.requestId, Response(BAD_REQUEST))

        checkPending()
        checkStatus(request.requestId, Success(Dead(Response(BAD_REQUEST))))
    }

    @Test
    fun `cannot mark request as dead after it has been processed`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

        markProcessed(request.requestId, Response(I_M_A_TEAPOT))
        markDead(request.requestId, Response(BAD_REQUEST), Failure(RequestAlreadyProcessed))

        checkPending()
    }

    @Test
    fun `check status of a request`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        checkStatus(request.requestId, Failure(RequestNotFound))

        store(request.requestId, request.request)

        checkStatus(request.requestId, Success(Pending))

        markProcessed(request.requestId, Response(I_M_A_TEAPOT), Success(Unit))

        checkStatus(request.requestId, Success(Processed(Response(I_M_A_TEAPOT))))
    }

    @Test
    fun `mark request as failed`() {
        val request = PostboxRequest(id(1), Request(GET, "/"))

        store(request.requestId, request.request)

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
        requestId: RequestId,
        request: Request,
        expectedStatus: Result<RequestProcessingStatus, PostboxError> = Success(Pending)
    ): PendingRequest {
        timeSource.tick(Duration.ofSeconds(1))
        val result = postbox.perform { it.store(requestId, request) }
        assertThat(result, equalTo(expectedStatus))
        return PendingRequest(requestId, request, timeSource())
    }

    private fun id(id: Int) = RequestId.of(id.toString())

}

