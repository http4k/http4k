package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.db.InMemoryTransactor
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.util.UUID

class TransactionalPostboxTest {
    private val postbox = TestPostbox()
    private val transactor = InMemoryTransactor<Postbox>(postbox)

    @Test
    fun `stores request for background processing`() {
        val aRequest = Request(POST, "/hello").body("hello")

        val interceptorResponse = TransactionalPostbox(transactor)(aRequest)
        assertThat(interceptorResponse, hasStatus(ACCEPTED))

        assertThat(postbox.pendingRequests(), equalTo(listOf(aRequest)))

        val postboxResponse = PostboxHandler(transactor)(Request(Method.GET, interceptorResponse.header("Link")!!))

        assertThat(postboxResponse, hasStatus(ACCEPTED))
    }

    @Test
    fun `handles storage failures`() {
        val postboxHandler = TransactionalPostbox(transactor)
        val aRequest = Request(POST, "/hello").body("hello")

        postbox.failNext()
        val response = postboxHandler(aRequest)

        assertThat(response, hasStatus(INTERNAL_SERVER_ERROR))

        assertThat(postbox.pendingRequests(), equalTo(emptyList()))
    }
}

class TestPostbox : Postbox {
    private val requests = mutableListOf<RequestWithId>()
    private var fail = false

    fun failNext() {
        fail = true
    }

    override fun store(request: Request): Result<RequestId, PostboxError> {
        return if (!fail) {
            val requestWithId = RequestWithId(request, RequestId.of(UUID.randomUUID().toString()))
            requests.add(requestWithId)
            Success(requestWithId.id)
        } else {
            fail = false;
            Failure(PostboxError.StorageFailure(IllegalStateException("Failed to store request")))
        }
    }

    override fun status(requestId: RequestId) =
        requests.find { it.id == requestId }?.let {
            Success(RequestProcessingStatus.Pending)
        } ?: Failure(PostboxError.RequestNotFound)

    fun pendingRequests() = requests.map { it.request }.toList()

    private data class RequestWithId(val request: Request, val id: RequestId)
}

