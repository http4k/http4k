package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.db.InMemoryTransactor
import org.junit.jupiter.api.Test
import java.util.*

abstract class PostboxContract {
    abstract val postbox: PostboxTransactor

    @Test
    fun `can store request in datasource`() {
        val pending = Postbox.PendingRequest(RequestId.of(UUID.randomUUID().toString()), Request(Method.GET, "/"))
        postbox.perform { it.store(pending) }

        postbox.perform {
            assertThat(it.pendingRequests(1), equalTo(listOf(pending)))
        }
    }
}

class InMemoryPostboxContract : PostboxContract() {
    override val postbox = InMemoryTransactor(InMemoryPostbox())
}

