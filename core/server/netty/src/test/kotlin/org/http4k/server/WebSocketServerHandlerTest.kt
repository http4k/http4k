package org.http4k.server

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.http4k.core.Method
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress

class WebSocketServerHandlerTest {

    private val address = InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1234)

    @Test
    fun `asRequest returns null for an unsupported HTTP method`() {
        val nettyRequest = DefaultHttpRequest(HTTP_1_1, HttpMethod.valueOf("BREW"), "/ws")

        assertThat(nettyRequest.asWsUpgradeRequest(address), absent())
    }

    @Test
    fun `asRequest parses a supported HTTP method`() {
        val nettyRequest = DefaultHttpRequest(HTTP_1_1, HttpMethod.GET, "/ws")

        val parsed = nettyRequest.asWsUpgradeRequest(address)

        assertThat(parsed, present(equalTo(parsed)))
        assertThat(parsed!!.method, equalTo(Method.GET))
    }
}
