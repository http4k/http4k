package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.time.Duration

class HttpEventTest {

    private val tx = HttpTransaction(Request(Method.GET, ""), Response(Status.OK), Duration.ZERO, mapOf())

    @Test
    fun `outgoing equals`() {
        assertThat(HttpEvent.Outgoing(tx), equalTo(HttpEvent.Outgoing(tx)))
    }

    @Test
    fun `incoming equals`() {
        assertThat(HttpEvent.Incoming(tx), equalTo(HttpEvent.Incoming(tx)))
    }

}
