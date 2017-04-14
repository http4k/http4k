package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.contract.Header
import org.reekwest.http.core.contract.Invalid
import org.reekwest.http.core.contract.Missing
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
        assertThat({ Header.required("world")(request) }, throws<Missing>())

        assertThat(Header.multi.optional("world")(request), equalTo(emptyList()))
        assertThat({ Header.multi.required("world")(request) }, throws<Missing>())
    }

    @Test
    fun `invalid value`() {
        assertThat({ Header.map(String::toInt).required("hello")(request) }, throws<Invalid>())
        assertThat({ Header.map(String::toInt).optional("hello")(request) }, throws<Invalid>())

        assertThat({ Header.map(String::toInt).multi.required("hello")(request) }, throws<Invalid>())
        assertThat({ Header.map(String::toInt).optional("hello")(request) }, throws<Invalid>())
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


    @Test
    fun `toString is ok`() {
        assertThat(Header.required("hello").toString(), equalTo("Required header 'hello'"))
        assertThat(Header.optional("hello").toString(), equalTo("Optional header 'hello'"))
        assertThat(Header.multi.required("hello").toString(), equalTo("Required header 'hello'"))
        assertThat(Header.multi.optional("hello").toString(), equalTo("Optional header 'hello'"))
    }

}