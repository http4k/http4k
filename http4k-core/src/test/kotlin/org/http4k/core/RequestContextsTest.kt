package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.util.UUID


class RequestContextsTest {

    @Test
    fun `can get and set and remove a request context using just the request`() {
        val requestContexts = RequestContexts()
        val value = RequestContext(UUID.randomUUID())
        val updated = requestContexts.inject(value, Request(Method.GET, ""))
        updated.header("x-http4k-context") shouldMatch equalTo(value.id.toString())

        requestContexts.extract(updated) shouldMatch equalTo(value)

        requestContexts.remove(value)

        assertThat({requestContexts.extract(updated)}, throws<IllegalStateException>())
    }

}