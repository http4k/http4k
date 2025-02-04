package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.postbox.Postbox.PendingRequest
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.exposed.ExposedPostbox
import org.jetbrains.exposed.sql.deleteAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

abstract class PostboxContract {
    abstract val postbox: PostboxTransactor

    @Test
    fun `can store request in datasource`() {
        val newRequest = PendingRequest(RequestId.of(UUID.randomUUID().toString()), Request(Method.GET, "/"))

        val result = postbox.perform { it.store(newRequest) }
        assertThat(result, equalTo(Success(Pending)))

        val pending = postbox.perform { it.pendingRequests(1) }
        assertThat(pending, equalTo(listOf(PendingRequest(newRequest.requestId, newRequest.request))))
    }

    @Test
    fun `store is idempotent`() {
        val requestId = RequestId.of(UUID.randomUUID().toString())
        val request1 = Request(Method.GET, "/foo")
        val request2 = Request(Method.GET, "/bar")

        val result = postbox.perform { it.store(PendingRequest(requestId, request1)) }
        assertThat(result, equalTo(Success(Pending)))

        val result2 = postbox.perform { it.store(PendingRequest(requestId, request2)) }
        assertThat(result2, equalTo(Success(Pending)))

        val pending = postbox.perform { it.pendingRequests(10) }
        assertThat(pending, equalTo(listOf(PendingRequest(requestId, request1))))
    }

    @Test
    fun `can mark request as processed`() {
        val request = PendingRequest(RequestId.of(UUID.randomUUID().toString()), Request(Method.GET, "/"))

        postbox.perform { it.store(request) }

        val result = postbox.perform { it.markProcessed(request.requestId, Response(I_M_A_TEAPOT)) }
        assertThat(result, equalTo(Success(Unit)))

        val pending = postbox.perform { it.pendingRequests(10) }
        assertThat(pending, equalTo(emptyList()))
    }

}

