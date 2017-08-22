package org.http4k.core

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.junit.Test
import java.nio.ByteBuffer

class BodyTest {
    @Test
    fun `body string`() {
        Response(Status.OK).body("abc").bodyString().shouldMatch(equalTo("abc"))
    }

    @Test
    fun `body bytebuffer`() {
        Response(Status.OK).body(Body(ByteBuffer.wrap("abc".toByteArray()))).bodyString()
                .shouldMatch(equalTo("abc"))
    }

    @Test
    fun `body stream`() {
        Response(Status.OK).body(Body("abc".byteInputStream())).bodyString()
                .shouldMatch(equalTo("abc"))
    }

    @Test
    fun `string body can be streamed`(){
        String(Body("abc").stream.readBytes()).shouldMatch(equalTo("abc"))
    }

}