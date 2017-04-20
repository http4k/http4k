package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.contract.ContractBreach.Companion.Invalid
import org.reekwest.http.contract.ContractBreach.Companion.Missing
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.get
import org.reekwest.http.core.header

class HeaderTest {
    private val request = Request(GET, uri("/"), listOf("hello" to "world", "hello" to "world2"))

    @Test
    fun `value present`() {
        assertThat(Header.optional("hello")(request), equalTo("world"))
        assertThat(Header.required("hello")(request), equalTo("world"))
        assertThat(Header.map { it.length }.required("hello")(request), equalTo(5))
        assertThat(Header.map { it.length }.optional("hello")(request), equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(Header.multi.required("hello")(request), equalTo(expected))
        assertThat(Header.multi.optional("hello")(request), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(Header.optional("world")(request), absent())
        val requiredHeader = Header.required("world")
        assertThat({ requiredHeader(request) }, throws(equalTo(Missing(requiredHeader))))

        assertThat(Header.multi.optional("world")(request), absent())
        val optionalMultiHeader = Header.multi.required("world")
        assertThat({ optionalMultiHeader(request) }, throws(equalTo(Missing(optionalMultiHeader))))
    }

    @Test
    fun `invalid value`() {
        val requiredHeader = Header.map(String::toInt).required("hello")
        assertThat({ requiredHeader(request) }, throws(equalTo(Invalid(requiredHeader))))

        val optionalHeader = Header.map(String::toInt).optional("hello")
        assertThat({ optionalHeader(request) }, throws(equalTo(Invalid(optionalHeader))))

        val requiredMultiHeader = Header.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiHeader(request) }, throws(equalTo(Invalid(requiredMultiHeader))))

        val optionalMultiHeader = Header.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiHeader(request) }, throws(equalTo(Invalid(optionalMultiHeader))))
    }

    @Test
    fun `int`() {
        val optionalHeader = Header.int().optional("hello")
        val requestWithHeader = withHeaderOf("123")
        assertThat(optionalHeader(requestWithHeader), equalTo(123))

        assertThat(Header.int().optional("world")(withHeaderOf("/")), absent())

        val badRequest = withHeaderOf("/?hello=notAnumber")
        assertThat({ optionalHeader(badRequest) }, throws(equalTo(Invalid(optionalHeader))))
    }

    @Test
    fun `sets value on request`() {
        val header = Header.required("bob")
        val withHeader = header("hello", request)
        assertThat(header(withHeader), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Header.map({ MyCustomBodyType(it) }, { it.value }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val reqWithHeader = custom(instance, get(""))

        assertThat(reqWithHeader.header("bob"), equalTo("hello world!"))

        assertThat(custom(reqWithHeader), equalTo(MyCustomBodyType("hello world!")))
    }

    private fun withHeaderOf(s: String) = Request(GET, uri("/"), listOf("hello" to s))

    @Test
    fun `toString is ok`() {
        assertThat(Header.required("hello").toString(), equalTo("Required header 'hello'"))
        assertThat(Header.optional("hello").toString(), equalTo("Optional header 'hello'"))
        assertThat(Header.multi.required("hello").toString(), equalTo("Required header 'hello'"))
        assertThat(Header.multi.optional("hello").toString(), equalTo("Optional header 'hello'"))
    }
}