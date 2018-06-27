package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri.Companion.of
import org.http4k.core.with
import org.junit.jupiter.api.Test

class HeaderTest {
    private val request = Request(GET, of("/")).header("hello", "world").header("hello", "world2")

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
        assertThat({ requiredHeader(request) }, throws(lensFailureWith(Missing(requiredHeader.meta), overallType = Failure.Type.Missing)))

        assertThat(Header.defaulted("world", "bob")(request), equalTo("bob"))
        val defaultedHeader = Header.defaulted("world", "bob")
        assertThat(defaultedHeader(request), equalTo("bob"))

        assertThat(Header.multi.optional("world")(request), absent())
        val optionalMultiHeader = Header.multi.required("world")
        assertThat({ optionalMultiHeader(request) }, throws(lensFailureWith(Missing(optionalMultiHeader.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `invalid value`() {
        val requiredHeader = Header.map(String::toInt).required("hello")
        assertThat({ requiredHeader(request) }, throws(lensFailureWith(Invalid(requiredHeader.meta), overallType = Failure.Type.Invalid)))

        val optionalHeader = Header.map(String::toInt).optional("hello")
        assertThat({ optionalHeader(request) }, throws(lensFailureWith(Invalid(optionalHeader.meta), overallType = Failure.Type.Invalid)))

        val requiredMultiHeader = Header.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiHeader(request) }, throws(lensFailureWith(Invalid(requiredMultiHeader.meta), overallType = Failure.Type.Invalid)))

        val optionalMultiHeader = Header.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiHeader(request) }, throws(lensFailureWith(Invalid(optionalMultiHeader.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `sets value on request`() {
        val header = Header.required("bob")
        val withHeader = request.with(header of "hello")
        assertThat(header(withHeader), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Header.map(::MyCustomBodyType, { it.value }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val reqWithHeader = custom(instance, Request(Method.GET, ""))

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

    @Test
    fun `content type serialises and deserialises correctly to message - with directive`() {
        val lens = Header.Common.CONTENT_TYPE
        val reqWithHeader = Request(GET, "").with(lens of ContentType.TEXT_HTML)
        reqWithHeader.header("Content-Type") shouldMatch equalTo("text/html; charset=utf-8")
        lens(reqWithHeader) shouldMatch equalTo(ContentType.TEXT_HTML)
    }

    @Test
    fun `content type serialises and deserialises correctly to message - with illegal directive is ignored`() {
        val lens = Header.Common.CONTENT_TYPE
        val reqWithHeader = Request(GET, "").header("Content-Type", "bob ; foomanchu")
        lens(reqWithHeader) shouldMatch equalTo(ContentType("bob"))
    }

    @Test
    fun `content type serialises and deserialises correctly to message - no directive`() {
        val lens = Header.Common.CONTENT_TYPE
        val reqWithHeader = Request(GET, "").with(lens of ContentType("value"))
        reqWithHeader.header("Content-Type") shouldMatch equalTo("value")
        lens(reqWithHeader) shouldMatch equalTo(ContentType("value"))
    }

}