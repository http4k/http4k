package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.hamkrest.hasBody
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test

class JettyClientStreamingTest : HttpClientContract(
    ::ApacheServer, JettyClient(bodyMode = BodyMode.Stream),
    JettyClient(bodyMode = BodyMode.Stream, requestModifier = timeout)
) {

    @Test
    override fun `can send multiple headers with same name`() {
        val response = client(
            Request(Method.POST, "http://localhost:$port/multiRequestHeader").header("echo", "foo")
                .header("echo", "bar")
        )

        assertThat(response, hasBody("echo: foo, bar"))
    }

    @Test
    override fun `socket timeouts are converted into 504`() {
    }
}
