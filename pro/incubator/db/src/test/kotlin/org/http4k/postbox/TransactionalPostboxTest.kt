package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
import org.http4k.postbox.Postbox.PendingRequest
import org.junit.jupiter.api.Test

class TransactionalPostboxTest {
    private val postbox = InMemoryPostbox()
    private val transactor = InMemoryTransactor<Postbox>(postbox)
    private val idFromUrl = { req: Request -> RequestId.of(req.uri.path.removePrefix("/")) }
    private val requestHandler = PostboxInterceptorHandler(transactor, idFromUrl)
    private val statusHandler = PostboxStatusHandler(transactor)

    @Test
    fun `stores request for background processing`() {
        val aRequest = Request(POST, "/hello").body("hello")

        val interceptorResponse = requestHandler(aRequest)
        assertThat(interceptorResponse, hasStatus(ACCEPTED))

        assertThat(postbox.pendingRequests(), equalTo(listOf(aRequest.asPending())))

        val postboxResponse = statusHandler(Request(GET, interceptorResponse.header("Link")!!))

        assertThat(postboxResponse, hasStatus(ACCEPTED))
    }

    @Test
    fun `returns response if request has been already processed`() {
        val aRequest = Request(POST, "/hello").body("hello")
        val aResponse = Response(OK).body("foo")

        postbox.store(aRequest.asPending())
        postbox.markProcessed(aRequest.id(), aResponse)

        val interceptorResponse = requestHandler(aRequest)
        assertThat(interceptorResponse, equalTo(aResponse))

        val postboxResponse = statusHandler(Request(GET, "/postbox/${aRequest.id()}"))

        assertThat(postboxResponse, equalTo(aResponse))
    }

    @Test
    fun `updates status of request`() {
        val aRequest = Request(POST, "/hello").body("hello")
        val finalServer = { request: Request -> Response(OK).body(request.body) }
        postbox.store(aRequest.asPending())

        ProcessPendingRequests(transactor, finalServer)

        val postboxResponse = statusHandler(Request(GET, "/postbox/${aRequest.id()}"))
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

    private fun Request.asPending() = PendingRequest(id(), this)
    private fun Request.id() = idFromUrl(this)
}
