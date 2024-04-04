package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.junit.jupiter.api.Test
import java.util.UUID

class RequestContextsTest {

    @Test
    fun `can get and set and remove a request context using just the request`() {
        val requestContexts = RequestContexts()
        val value = RequestContext(UUID.randomUUID())
        val updated = requestContexts.inject(value, Request(GET, ""))
        assertThat(updated.header("x-http4k-context"), equalTo(value.id.toString()))

        assertThat(requestContexts.extract(updated), equalTo(value))

        requestContexts.remove(value)

        assertThat({ requestContexts.extract(updated) }, throws<IllegalStateException>())
    }

    @Test
    fun `you can roll your own`() {
        @Suppress("unused")
        class MyVeryOwnContextStore : Store<RequestContext> {
            override fun invoke(target: Request): RequestContext = TODO()
            override fun <R : Request> invoke(value: RequestContext, target: R): R {
                @Suppress("UNUSED_VARIABLE")
                val id = value.id
                TODO()
            }

            override fun remove(value: RequestContext): RequestContext? = TODO()
        }
    }
}
