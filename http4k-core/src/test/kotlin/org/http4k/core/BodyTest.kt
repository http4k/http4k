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

    @Test
    fun `stream body allow for equality by consuming its stream`(){
        Body("abc".byteInputStream()).shouldMatch(equalTo(Body("abc".byteInputStream())))
    }

    @Test
    fun `can consume stream body as string more than once`(){
        val body = Body("abc".byteInputStream())
        body.toString()
        body.toString().shouldMatch(equalTo("abc"))
    }

    @Test
    fun `can not consume stream body as stream more than once`(){
        val body = Body("abc".byteInputStream())
        String(body.stream.readBytes()) shouldMatch equalTo("abc")
        String(body.stream.readBytes()) shouldMatch equalTo("")
    }

    @Test
    fun `stream body generates consistent hashing by consuming its stream`(){
        Body("abc".byteInputStream()).hashCode().shouldMatch(equalTo(Body("abc".byteInputStream()).hashCode()))
    }

}