package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.filter.CanonicalPayload
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class AwsCanonicalRequestTest {

    private val canonicalPayload =
        CanonicalPayload(AwsHmacSha256.hash(Body.EMPTY.payload.array()), Body.EMPTY.payload.array().size.toLong())

    @Test
    fun `creates canonical version of simple request`() {
        val canonical = AwsCanonicalRequest.of(Request(GET, "http://www.google.com/a/b").query("foo", "bar").header("abc", "def"), canonicalPayload)
        assertThat(canonical.value, equalTo("""GET
/a/b
foo=bar
abc:def

abc
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `creates canonical version of request with null parameter`() {
        val canonical = AwsCanonicalRequest.of(Request(GET, "http://www.google.com/a/b").query("foo", null), canonicalPayload)
        assertThat(canonical.value, equalTo("""GET
/a/b
foo=



e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `normalises path`() {
        val canonical = AwsCanonicalRequest.of(Request(GET, "http://www.google.com/a:b:c/d e/f"), canonicalPayload)
        assertThat(canonical.value, equalTo("""GET
/a%3Ab%3Ac/d+e/f




e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `normalises empty path`() {
        val canonical = AwsCanonicalRequest.of(Request(GET, "http://www.google.com"), canonicalPayload)
        assertThat(canonical.value, equalTo("""GET
/




e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `generates payload hash of binary body`() {
        val image = this::class.java.getResourceAsStream("/test.png").readBytes()
        val body = Body(ByteBuffer.wrap(image))
        val canonical = AwsCanonicalRequest.of(Request(GET, "http://www.google.com").body(body), CanonicalPayload(AwsHmacSha256.hash(body.payload.array()), body.payload.array().size.toLong()))
        assertThat(canonical.value, equalTo("""GET
/




0fa4d114b9fbb132f096d727713aab9ea8d415b69c86053b6d2c819c4eb95db6"""))
    }
}
