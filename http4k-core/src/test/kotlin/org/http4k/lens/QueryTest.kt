package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Request.Companion.get
import org.http4k.core.Uri.Companion.uri
import org.junit.Test

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

        val requiredQuery = Query.required("world")
        assertThat({ requiredQuery(request) }, throws(equalTo(LensFailure(requiredQuery.missing()))))

        assertThat(Query.multi.optional("world")(request), absent())
        val requiredMultiQuery = Query.multi.required("world")
        assertThat({ requiredMultiQuery(request) }, throws(equalTo(LensFailure(requiredMultiQuery.missing()))))
    }

    @Test
    fun `invalid value`() {
        val requiredQuery = Query.map(String::toInt).required("hello")
        assertThat({ requiredQuery(request) }, throws(equalTo(LensFailure(requiredQuery.invalid()))))

        val optionalQuery = Query.map(String::toInt).optional("hello")
        assertThat({ optionalQuery(request) }, throws(equalTo(LensFailure(optionalQuery.invalid()))))

        val requiredMultiQuery = Query.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiQuery(request) }, throws(equalTo(LensFailure(requiredMultiQuery.invalid()))))

        val optionalMultiQuery = Query.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiQuery(request) }, throws(equalTo(LensFailure(optionalMultiQuery.invalid()))))
    }

    @Test
    fun `sets value on request`() {
        val query = Query.required("bob")
        val withQuery = query("hello", request)
        assertThat(query(withQuery), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Query.map(::MyCustomBodyType, { it.value }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val reqWithQuery = custom(instance, get(""))

        assertThat(reqWithQuery.query("bob"), equalTo("hello world!"))

        assertThat(custom(reqWithQuery), equalTo(MyCustomBodyType("hello world!")))
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