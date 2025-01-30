package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.db.InMemoryTransactor
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class TransactionalPostboxTest {
    private val postbox = TestPostbox()
    private val transactor = InMemoryTransactor<Postbox>(postbox)
    private val idFromUrl = { req: Request -> RequestId.of(req.uri.path.removePrefix("/")) }
    private val requestHandler = TransactionalPostbox(transactor, idFromUrl)
    private val statusHandler = PostboxStatusHandler(transactor)

    @Test
    fun `stores request for background processing`() {
        val aRequest = Request(POST, "/hello").body("hello")

        val interceptorResponse = requestHandler(aRequest)
        assertThat(interceptorResponse, hasStatus(ACCEPTED))

        assertThat(postbox.pendingRequests(), equalTo(listOf(aRequest)))

        val postboxResponse = statusHandler(Request(GET, interceptorResponse.header("Link")!!))

        assertThat(postboxResponse, hasStatus(ACCEPTED))
    }

    @Test
    fun `returns response if request has been already processed`() {
        val aRequest = Request(POST, "/hello").body("hello")
        val aResponse = Response(OK).body("foo")

        postbox.store(idFromUrl(aRequest), aRequest)
        postbox.markProcessed(idFromUrl(aRequest), aResponse)

        val interceptorResponse = requestHandler(aRequest)
        assertThat(interceptorResponse, equalTo(aResponse))

        val postboxResponse = statusHandler(Request(GET, "/postbox/${idFromUrl(aRequest)}"))

        assertThat(postboxResponse, equalTo(aResponse))
    }

    @Test
    fun `updates status of request`() {
        val aRequest = Request(POST, "/hello").body("hello")
        val finalServer = { request: Request -> Response(OK).body(request.body) }
        postbox.store(idFromUrl(aRequest), aRequest)

        val result = transactor.perform { ProcessRequest(it, idFromUrl(aRequest), aRequest, finalServer) }
        assertThat(result, equalTo(Success(Unit)))

        val postboxResponse = statusHandler(Request(GET, "/postbox/${idFromUrl(aRequest)}"))
        assertThat(postboxResponse, equalTo(Response(OK).body("hello")))
    }

    @Test
    fun `handles storage failures`() {
        val postboxHandler = requestHandler
        val aRequest = Request(POST, "/hello").body("hello")

        postbox.failNext()
        val response = postboxHandler(aRequest)

        assertThat(response, hasStatus(INTERNAL_SERVER_ERROR))

        assertThat(postbox.pendingRequests(), equalTo(emptyList()))
    }

    @Test
    fun `handles status for unknown request`() {
        assertThat(statusHandler(Request(GET, "/postbox/unknown")), hasStatus(NOT_FOUND))
    }
}

class TestPostbox : Postbox {
    private val requests = mutableMapOf<RequestId, Pair<Request, Response?>>()

    private var fail = false

    fun failNext() {
        fail = true
    }

    private fun findRequest(requestId: RequestId) = requests[requestId]

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> {
        return if (!fail) {
            val existingRequest = findRequest(requestId)
            if (existingRequest == null) {
                requests[requestId] = request to null
                Success(RequestProcessingStatus.Pending)
            } else {
                val response = existingRequest.second
                if (response == null) {
                    Success(RequestProcessingStatus.Pending)
                } else {
                    Success(RequestProcessingStatus.Processed(response))
                }
            }
        } else {
            fail = false;
            Failure(PostboxError.StorageFailure(IllegalStateException("Failed to store request")))
        }
    }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> =
        findRequest(requestId)?.let {
            requests[requestId] = it.first to response
            Success(Unit)
        } ?: Failure(PostboxError.RequestNotFound)

    override fun status(requestId: RequestId) =
        findRequest(requestId)?.let {
            when {
                it.second != null -> Success(RequestProcessingStatus.Processed(it.second!!))
                else -> Success(RequestProcessingStatus.Pending)
            }
        } ?: Failure(PostboxError.RequestNotFound)

    fun pendingRequests() = requests.values.map { it.first }.toList()

}

