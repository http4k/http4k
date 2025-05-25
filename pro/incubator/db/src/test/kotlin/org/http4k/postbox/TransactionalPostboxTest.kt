package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.FixedTimeSource
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
import org.http4k.postbox.PendingResponseGenerators.linkHeader
import org.http4k.postbox.Postbox.PendingRequest
import org.http4k.postbox.RequestIdResolvers.fromPath
import org.http4k.postbox.storage.inmemory.InMemoryPostbox
import org.http4k.postbox.processing.PostboxProcessing
import org.junit.jupiter.api.Test

class TransactionalPostboxTest {
    private val timeSource = FixedTimeSource()
    private val postbox = InMemoryPostbox(timeSource)
    private val transactor = InMemoryTransactor<Postbox>(postbox)
    private val processing = PostboxProcessing(transactor, { request -> Response(OK).body(request.body) })
    private val handlers = PostboxHandlers(transactor, linkHeader("requestId"))
    private val requestHandler = handlers.intercepting(fromPath("requestId"))
    private val statusHandler = handlers.status(fromPath("requestId"))

    @Test
    fun `stores request for background processing`() = runBlocking {
        val aRequest = Request(POST, "/hello").body("hello")

        val interceptorResponse = requestHandler(aRequest)
        assertThat(interceptorResponse, hasStatus(ACCEPTED))

        assertThat(postbox.pendingRequests(10, timeSource()), equalTo(listOf(PendingRequest(aRequest.id(), aRequest, timeSource(), 0))))

        val postboxResponse = statusHandler(Request(GET, interceptorResponse.header("Link")!!))

        assertThat(postboxResponse, hasStatus(ACCEPTED))
    }

    @Test
    fun `returns response if request has been already processed`() = runBlocking {
        val aRequest = Request(POST, "/hello").body("hello")
        val aResponse = Response(OK).body("foo")

        postbox.store(aRequest.id(), aRequest)
        postbox.markProcessed(aRequest.id(), aResponse)

        val interceptorResponse = requestHandler(aRequest)
        assertThat(interceptorResponse, equalTo(aResponse))

        val postboxResponse = statusHandler(Request(GET, "/${aRequest.id()}"))

        assertThat(postboxResponse, equalTo(aResponse))
    }

    @Test
    fun `updates status of request`() = runBlocking {
        val aRequest = Request(POST, "/hello").body("hello")
        postbox.store(aRequest.id(), aRequest)

        processing.processPendingRequests({ it.status.successful })

        val postboxResponse = statusHandler(Request(GET, "/${aRequest.id()}"))
        assertThat(postboxResponse, equalTo(Response(OK).body("hello")))
    }

    @Test
    fun `handles storage failures`() = runBlocking {
        val postboxHandler = requestHandler
        val aRequest = Request(POST, "/hello").body("hello")

        postbox.failNext()
        val response = postboxHandler(aRequest)

        assertThat(response, hasStatus(INTERNAL_SERVER_ERROR))

        assertThat(postbox.pendingRequests(10, timeSource()), equalTo(emptyList()))
    }

    @Test
    fun `handles status for unknown request`() = runBlocking {
        assertThat(statusHandler(Request(GET, "/unknown")), hasStatus(NOT_FOUND))
    }

    private fun Request.id() = fromPath("requestId")(this) ?: error("No id found")
}
