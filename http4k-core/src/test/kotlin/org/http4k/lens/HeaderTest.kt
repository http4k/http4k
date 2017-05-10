package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Request.Companion.get
import org.http4k.core.Uri.Companion.of
import org.junit.Test

class HeaderTest {
    private val request = Request(GET, of("/"), listOf("hello" to "world", "hello" to "world2"))

    @Test
    fun `value present`() {
        assertThat(Header.optional("hello")(request), equalTo("world"))
        assertThat(Header.required("hello")(request), equalTo("world"))
        assertThat(Header.defaulted("hello", "moon")(request), equalTo("world"))
        assertThat(Header.map { it.length }.required("hello")(request), equalTo(5))
        assertThat(Header.map { it.length }.optional("hello")(request), equalTo(5))
        assertThat(Header.map { it.length }.defaulted("hello", 2)(request), equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(Header.multi.required("hello")(request), equalTo(expected))
        assertThat(Header.multi.optional("hello")(request), equalTo(expected))
        assertThat(Header.multi.defaulted("hello", listOf("foo"))(request), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(Header.optional("world")(request), absent())
        val requiredHeader = Header.required("world")
        assertThat({ requiredHeader(request) }, throws(equalTo(LensFailure(requiredHeader.missing()))))

        assertThat(Header.defaulted("world", "bob")(request), equalTo("bob"))
        val defaultedHeader = Header.defaulted("world", "bob")
        assertThat(defaultedHeader(request), equalTo("bob"))

        assertThat(Header.multi.optional("world")(request), absent())
        val optionalMultiHeader = Header.multi.required("world")
        assertThat({ optionalMultiHeader(request) }, throws(equalTo(LensFailure(optionalMultiHeader.missing()))))
    }

    @Test
    fun `invalid value`() {
        val requiredHeader = Header.map(String::toInt).required("hello")
        assertThat({ requiredHeader(request) }, throws(equalTo(LensFailure(requiredHeader.invalid()))))

        val optionalHeader = Header.map(String::toInt).optional("hello")
        assertThat({ optionalHeader(request) }, throws(equalTo(LensFailure(optionalHeader.invalid()))))

        val requiredMultiHeader = Header.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiHeader(request) }, throws(equalTo(LensFailure(requiredMultiHeader.invalid()))))

        val optionalMultiHeader = Header.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiHeader(request) }, throws(equalTo(LensFailure(optionalMultiHeader.invalid()))))
    }

    @Test
    fun `sets value on request`() {
        val header = Header.required("bob")
        val withHeader = header("hello", request)
        assertThat(header(withHeader), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Header.map(::MyCustomBodyType, { it.value }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val reqWithHeader = custom(instance, get(""))

        assertThat(reqWithHeader.header("bob"), equalTo("hello world!"))

        assertThat(custom(reqWithHeader), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(Header.required("hello").toString(), equalTo("Required header 'hello'"))
        assertThat(Header.optional("hello").toString(), equalTo("Optional header 'hello'"))
        assertThat(Header.multi.required("hello").toString(), equalTo("Required header 'hello'"))
        assertThat(Header.multi.optional("hello").toString(), equalTo("Optional header 'hello'"))
    }
}