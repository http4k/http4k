package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.contract.Invalid
import org.reekwest.http.core.contract.Missing
import org.reekwest.http.core.contract.Query
import org.reekwest.http.core.contract.int

class QueryTest {
    private val request = withQueryOf("/?hello=world&hello=world2")

    @Test
    fun `value present`() {
        assertThat(Query.optional("hello")(request), equalTo("world"))
        assertThat(Query.required("hello")(request), equalTo("world"))
        assertThat(Query.map { it.length }.required("hello")(request), equalTo(5))
        assertThat(Query.map { it.length }.optional("hello")(request), equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(Query.multi.required("hello")(request), equalTo(expected))
        assertThat(Query.multi.optional("hello")(request), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(Query.optional("world")(request), absent())
        assertThat({ Query.required("world")(request) }, throws<Missing>())

        assertThat(Query.multi.optional("world")(request), equalTo(emptyList()))
        assertThat({ Query.multi.required("world")(request) }, throws<Missing>())
    }

    @Test
    fun `invalid value`() {
        assertThat({ Query.map(String::toInt).required("hello")(request) }, throws<Invalid>())
        assertThat({ Query.map(String::toInt).optional("hello")(request) }, throws<Invalid>())

        assertThat({ Query.map(String::toInt).multi.required("hello")(request) }, throws<Invalid>())
        assertThat({ Query.map(String::toInt).optional("hello")(request) }, throws<Invalid>())
    }

    @Test
    fun `int`() {
        assertThat(Query.int().optional("hello")(withQueryOf("/?hello=123")), equalTo(123))
        assertThat(Query.int().optional("world")(withQueryOf("/")), absent())
        val badRequest = withQueryOf("/?hello=notAnumber")
        assertThat({ Query.int().optional("hello")(badRequest) }, throws<Invalid>())
    }

    @Test
    fun `sets value on request`() {
        val query = Query.required("bob")
        val withQuery = query(request, "hello")
        assertThat(query(withQuery), equalTo("hello"))
    }

    private fun withQueryOf(value: String) = Request(GET, uri(value))

    @Test
    fun `toString is ok`() {
        assertThat(Query.required("hello").toString(), equalTo("Required query 'hello'"))
        assertThat(Query.optional("hello").toString(), equalTo("Optional query 'hello'"))
        assertThat(Query.multi.required("hello").toString(), equalTo("Required query 'hello'"))
        assertThat(Query.multi.optional("hello").toString(), equalTo("Optional query 'hello'"))
    }
}