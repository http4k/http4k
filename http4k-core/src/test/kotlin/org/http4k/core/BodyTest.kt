package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class BodyTest {
    @Test
    fun `body string`() {
        assertThat(Response(OK).body("abc").bodyString(), equalTo("abc"))
    }

    @Test
    fun `body bytebuffer`() {
        assertThat(Response(OK).body(Body(ByteBuffer.wrap("abc".toByteArray()))).bodyString(),
            equalTo("abc"))
    }

    @Test
    fun `body stream`() {
        assertThat(String(Response(OK).body(Body("abc".byteInputStream())).body.stream.readBytes()),
            equalTo("abc"))
    }

    @Test
    fun `string body can be streamed`() {
        assertThat(String(Body("abc").stream.readBytes()), equalTo("abc"))
    }

    @Test
    fun `stream body allow for equality by consuming its stream`() {
        assertThat(Body("abc".byteInputStream()), equalTo(Body("abc".byteInputStream())))
    }

    @Test
    fun `can consume stream body as payload more than once`() {
        val body = Body("abc".byteInputStream())
        String(body.payload.array())
        assertThat(String(body.payload.array()), equalTo("abc"))
    }

    @Test
    fun `can not consume stream body as stream more than once`() {
        val body = Body("abc".byteInputStream())
        assertThat(String(body.stream.readBytes()), equalTo("abc"))
        assertThat(String(body.stream.readBytes()), equalTo(""))
    }

    @Test
    fun `stream body generates consistent hashing by consuming its stream`() {
        assertThat(Body("abc".byteInputStream()).hashCode(), equalTo(Body("abc".byteInputStream()).hashCode()))
    }

    @Test
    fun `can construct with array backed ByteBuffer`() {
        val body = Body(ByteBuffer.wrap("abc".toByteArray()))
        assertThat(body.length, equalTo(3L))
        assertThat(body.toString(), equalTo("abc"))
        assertThat(body.stream.bufferedReader().use { it.readText() }, equalTo("abc"))
    }

    @Test
    fun `can construct with read only array backed ByteBuffer`() {
        val body = Body(ByteBuffer.wrap("abc".toByteArray()).asReadOnlyBuffer())
        assertThat(body.length, equalTo(3L))
        assertThat(body.toString(), equalTo("abc"))
        assertThat(body.stream.bufferedReader().use { it.readText() }, equalTo("abc"))
    }

    @Test
    fun `can construct with non-array backed ByteBuffer`() {
        val bytes = "abc".toByteArray()
        val buf = ByteBuffer.allocateDirect(bytes.size)
        buf.put(bytes)
        buf.flip()

        val body = Body(buf)
        assertThat(body.length, equalTo(3L))
        assertThat(body.toString(), equalTo("abc"))
        assertThat(body.stream.bufferedReader().use { it.readText() }, equalTo("abc"))
    }

    @Test
    fun `correct length and bytes when created from ByteBuffer`() {
        val bytes = ByteArray(16) { it.toByte() }
        val buf = ByteBuffer.wrap(bytes, 4, 8)
        val body = Body(buf)

        assertThat("body length", body.length, equalTo(8))

        val bodyContents = body.stream.readAllBytes()

        assertThat("body contents", bodyContents.toList(), equalTo(byteArrayOf(4,5,6,7,8,9,10,11).toList()))
    }

    @Test
    fun `correct string contents when created from ByteBuffer`() {
        val bytes = "abcdefghij".toByteArray(Charsets.UTF_8)
        val body = Body(ByteBuffer.wrap(bytes, 2, 4))

        assertThat("body to string", body.toString(), equalTo("cdef"))
    }
}
