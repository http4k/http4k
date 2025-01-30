package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.db.InMemoryTransactor
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class TransactionalPostboxTest {
    private val postbox = TestPostbox()
    private val transactor = InMemoryTransactor<Postbox>(postbox)

    @Test
    fun `stores request for background processing`() {
        val postboxHandler = TransactionalPostbox(transactor)
        val aRequest = Request(POST, "/hello").body("hello")

        val response = postboxHandler(aRequest)

        assertThat(response, hasStatus(ACCEPTED))

        assertThat(postbox.pendingRequests(), equalTo(listOf(aRequest)))
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
    private val requests = mutableListOf<Request>()
    private var fail = false

    fun failNext() {
        fail = true
    }

    override fun store(request: Request) {
        if (!fail) {
            requests.add(request)
        } else {
            fail = false;
            error("Failed to store request")
        }
    }

    fun pendingRequests() = requests.toList()

}

