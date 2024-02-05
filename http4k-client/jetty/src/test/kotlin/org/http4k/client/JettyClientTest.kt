package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.hamkrest.hasBody
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test

class JettyClientTest :
    HttpClientContract(::ApacheServer, JettyClient(), JettyClient(requestModifier = timeout)),
    HttpClientWithMemoryModeContract {

    @Test
    override fun `can send multiple headers with same name`() {
        val response = client(Request(Method.POST, "http://localhost:$port/multiRequestHeader").header("echo", "foo").header("echo", "bar"))

        assertThat(response, hasBody("echo: foo, bar"))
    }

}
