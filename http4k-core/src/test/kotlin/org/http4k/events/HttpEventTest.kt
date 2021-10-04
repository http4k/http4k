package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO

class HttpEventTest {

    private val tx = HttpTransaction(Request(GET, ""), Response(OK), ZERO, mapOf())

    @Test
    fun `outgoing equals`() {
        assertThat(HttpEvent.Outgoing(tx), equalTo(HttpEvent.Outgoing(tx)))
    }

    @Test
    fun `incoming equals`() {
        assertThat(HttpEvent.Incoming(tx), equalTo(HttpEvent.Incoming(tx)))
    }

}
