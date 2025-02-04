package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.db.InMemoryTransactor
import org.junit.jupiter.api.Test
import java.util.*

abstract class PostboxContract {
    abstract val postbox: PostboxTransactor

    @Test
    fun `can store request in datasource`() {
        val newRequest = Postbox.PendingRequest(RequestId.of(UUID.randomUUID().toString()), Request(Method.GET, "/"))

        val result = postbox.perform { it.store(newRequest) }
        assertThat(result, equalTo(Success(RequestProcessingStatus.Pending)))

        val pending = postbox.perform { it.pendingRequests(1) }
        assertThat(pending, equalTo(listOf(Postbox.PendingRequest(newRequest.requestId, newRequest.request))))

    }

    @Test
    fun `store is idempotent`() {
        val requestId = RequestId.of(UUID.randomUUID().toString())
        val request1 = Request(Method.GET, "/foo")
        val request2 = Request(Method.GET, "/bar")

        val result = postbox.perform { it.store(Postbox.PendingRequest(requestId, request1)) }
        assertThat(result, equalTo(Success(RequestProcessingStatus.Pending)))

        val result2 = postbox.perform { it.store(Postbox.PendingRequest(requestId, request2)) }
        assertThat(result2, equalTo(Success(RequestProcessingStatus.Pending)))

        val pending = postbox.perform { it.pendingRequests(10) }
        assertThat(pending, equalTo(listOf(Postbox.PendingRequest(requestId, request1))))
    }

}

class InMemoryPostboxContract : PostboxContract() {
    override val postbox = InMemoryTransactor(InMemoryPostbox())
}

