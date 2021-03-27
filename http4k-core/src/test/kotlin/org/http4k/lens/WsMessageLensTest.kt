package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class WsMessageLensTest {

    @Test
    fun `can get string wsMessage`() {
        assertThat((WsMessage.string().toLens())(WsMessage("some value")), equalTo("some value"))
    }

    @Test
    fun `can get binary wsMessage`() {
        assertThat((WsMessage.binary().toLens())(WsMessage("some value")), equalTo(ByteBuffer.wrap("some value".toByteArray())))
    }

    @Test
    fun `sets value on request`() {
        val wsMessage = WsMessage.string().toLens()
        val withBody = wsMessage("hello")
        assertThat(wsMessage(withBody), equalTo("hello"))
    }

    @Test
    fun `synonym methods roundtrip`() {
        val wsMessage = WsMessage.string().toLens()

        val withBody: WsMessage = wsMessage.create("hello")
        assertThat(wsMessage.extract(withBody), equalTo("hello"))
    }

    @Test
    fun `failures produce LensFailure`() {
        val wsMessage = WsMessage.string().map(String::toInt).toLens()
        assertThat({ wsMessage.extract(WsMessage("hello")) }, throws<LensFailure>())
    }

    @Test
    fun `can create a custom wsMessage type and get and set on request`() {
        val customBody = WsMessage.string().map(::MyCustomType, { it.value }).toLens()

        val custom = MyCustomType("hello world!")
        val reqWithBody = customBody(custom)

        assertThat(reqWithBody.bodyString(), equalTo("hello world!"))

        assertThat(customBody(reqWithBody), equalTo(MyCustomType("hello world!")))
    }

    @Test
    fun `can create a one way custom wsMessage type`() {
        val customBody = WsMessage.string().map(::MyCustomType).toLens()
        assertThat(customBody(WsMessage("hello world!")), equalTo(MyCustomType("hello world!")))
    }
}
