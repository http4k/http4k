package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import org.junit.Test
import java.time.Duration.ZERO

class HttpTransactionTest {

    @Test
    fun `can get the routing group`() {
        assertThat(HttpTransaction(Request(GET, Uri.of("/")), Response(OK), ZERO).routingGroup, equalTo("UNMAPPED"))

        assertThat(HttpTransaction(Request(GET, Uri.of("/"))
            .with(Header.X_URI_TEMPLATE of "hello"), Response(OK), ZERO).routingGroup, equalTo("hello"))
    }

}