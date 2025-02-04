package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.postbox.Postbox.PendingRequest
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.junit.jupiter.api.Test

abstract class PostboxContract {
    abstract val postbox: PostboxTransactor

    @Test
    fun `can store request in datasource`() {
        val newRequest = PendingRequest(id(1), Request(GET, "/"))

        store(newRequest)

        checkPending(newRequest)
    }

    @Test
    fun `store is idempotent`() {
        val requestId = id(1)
        val request1 = Request(GET, "/foo")
        val request2 = Request(GET, "/bar")

        store(PendingRequest(requestId, request1))
        store(PendingRequest(requestId, request2))

        checkPending(PendingRequest(requestId, request1))
    }

    @Test
    fun `multiple attempts to store the same request do not affect pending retrieval order`() {
        val newRequestOne = PendingRequest(id(1), Request(GET, "/foo"))
        val newRequestTwo = PendingRequest(id(2), Request(GET, "/bar"))

        store(newRequestOne)
        store(newRequestTwo)

        checkPending(newRequestOne, newRequestTwo)

        store(newRequestOne)

        checkPending(newRequestOne, newRequestTwo)
    }

    @Test
    fun `can mark request as processed`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        store(request)

        markProcessed(request.requestId, Response(I_M_A_TEAPOT))

        checkPending()
    }

    @Test
    fun `can check status of a request`() {
        val request = PendingRequest(id(1), Request(GET, "/"))

        val status1 = postbox.perform { it.status(request.requestId) }
        assertThat(status1, equalTo(Failure(RequestNotFound)))

        store(request)

        val status = postbox.perform { it.status(request.requestId) }
        assertThat(status, equalTo(Success(Pending)))

        markProcessed(request.requestId, Response(I_M_A_TEAPOT))

        val status3 = postbox.perform { it.status(request.requestId) }
        assertThat(status3, equalTo(Success(RequestProcessingStatus.Processed(Response(I_M_A_TEAPOT)))))

    }

    private fun markProcessed(requestId: RequestId, response: Response) {
        val result = postbox.perform { it.markProcessed(requestId, response) }
        assertThat(result, equalTo(Success(Unit)))
    }

    private fun checkPending(vararg expected: PendingRequest) {
        val pending = postbox.perform { it.pendingRequests(10) }
        assertThat(pending, equalTo(expected.toList()))
    }

    private fun store(request: PendingRequest) {
        val result = postbox.perform { it.store(request) }
        assertThat(result, equalTo(Success(Pending)))
    }

    private fun id(id: Int) = RequestId.of(id.toString())

}

