package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri.Companion.of
import org.http4k.core.query
import org.http4k.core.with
import org.junit.jupiter.api.Test

class QueryTest {
    private val request = Request(GET, of("/?hello=world&hello=world2"))

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
        assertThat({ requiredQuery(request) }, throws(lensFailureWith<Request>(Missing(requiredQuery.meta), overallType = Failure.Type.Missing)))

        assertThat(Query.multi.optional("world")(request), absent())
        val requiredMultiQuery = Query.multi.required("world")
        assertThat({ requiredMultiQuery(request) }, throws(lensFailureWith<Request>(Missing(requiredMultiQuery.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `value replaced`() {
        val single = Query.required("world")
        assertThat(single("value2", single("value1", request)), equalTo(request.query("world", "value2")))

        val multi = Query.multi.required("world")
        assertThat(multi(listOf("value3", "value4"), multi(listOf("value1", "value2"), request)),
            equalTo(request.query("world", "value3").query("world", "value4")))
    }

    @Test
    fun `invalid value`() {
        val requiredQuery = Query.map(String::toInt).required("hello")
        assertThat({ requiredQuery(request) }, throws(lensFailureWith<Request>(Invalid(requiredQuery.meta), overallType = Failure.Type.Invalid)))

        val optionalQuery = Query.map(String::toInt).optional("hello")
        assertThat({ optionalQuery(request) }, throws(lensFailureWith<Request>(Invalid(optionalQuery.meta), overallType = Failure.Type.Invalid)))

        val requiredMultiQuery = Query.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiQuery(request) }, throws(lensFailureWith<Request>(Invalid(requiredMultiQuery.meta), overallType = Failure.Type.Invalid)))

        val optionalMultiQuery = Query.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiQuery(request) }, throws(lensFailureWith<Request>(Invalid(optionalMultiQuery.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `sets value on request`() {
        val query = Query.required("bob")
        val withQuery = request.with(query of "hello")
        assertThat(query(withQuery), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Query.map(::MyCustomType) { it.value }.required("bob")

        val instance = MyCustomType("hello world!")
        val reqWithQuery = custom(instance, Request(GET, ""))

        assertThat(reqWithQuery.query("bob"), equalTo("hello world!"))

        assertThat(custom(reqWithQuery), equalTo(MyCustomType("hello world!")))
    }

    @Test
    fun `required custom type with null value`() {
        val nonMapped = Query.required("bob")

        val request = Request(GET, "/foo")
        assertThat(request.query("bob"), absent())

        val requestWithNullQueryValue = request.uri(request.uri.query("bob", null))

        assertThat({ nonMapped(request) }, throws<LensFailure>())
        assertThat(nonMapped(requestWithNullQueryValue), present(equalTo("")))
    }

    @Test
    fun `optional custom type with null value`() {
        val mapped = Query.map(::MyCustomType) { it.value }.optional("bob")
        val nonMapped = Query.optional("bob")

        val request = Request(GET, "/foo")
        assertThat(request.query("bob"), absent())
        assertThat(request.queries("bob"), equalTo(emptyList()))

        val requestWithNullQueryValue = request.uri(request.uri.query("bob", null))
        assertThat(requestWithNullQueryValue.uri.toString(), equalTo("/foo?bob"))
        assertThat(requestWithNullQueryValue.query("bob"), absent())
        assertThat(requestWithNullQueryValue.queries("bob"), equalTo(listOf<String?>(null)))
        assertThat(mapped(requestWithNullQueryValue), present(equalTo(MyCustomType(""))))
        assertThat(nonMapped(requestWithNullQueryValue), equalTo(""))

        val requestWithEmptyMappedType = request.with(mapped of MyCustomType(""))
        assertThat(requestWithEmptyMappedType.uri.toString(), equalTo("/foo?bob="))
        assertThat(requestWithEmptyMappedType.query("bob"), equalTo(""))
        assertThat(requestWithEmptyMappedType.queries("bob"), equalTo(listOf<String?>("")))
        assertThat(mapped(requestWithEmptyMappedType), equalTo(MyCustomType("")))
        assertThat(nonMapped(requestWithEmptyMappedType), equalTo(""))

        val requestWithNullMappedType = request.with(mapped of null)
        assertThat(requestWithNullMappedType.uri.toString(), equalTo("/foo"))
        assertThat(requestWithNullMappedType.query("bob"), absent())
        assertThat(requestWithNullMappedType.queries("bob"), equalTo(emptyList()))
        assertThat(mapped(requestWithNullMappedType), absent())
        assertThat(nonMapped(requestWithNullMappedType), absent())
    }

    @Test
    fun `toString is ok`() {
        assertThat(Query.required("hello").toString(), equalTo("Required query 'hello'"))
        assertThat(Query.optional("hello").toString(), equalTo("Optional query 'hello'"))
        assertThat(Query.multi.required("hello").toString(), equalTo("Required query 'hello'"))
        assertThat(Query.multi.optional("hello").toString(), equalTo("Optional query 'hello'"))
    }

    @Test
    fun `enum`() {
        val requiredLens = Query.enum<Method>().required("method")
        assertThat(requiredLens(Request(GET, "/?method=DELETE")), equalTo(Method.DELETE))

        val optionalLens = Query.enum<Method>().optional("method")
        assertThat(optionalLens(Request(GET, "/?method=DELETE")), equalTo(Method.DELETE))
        assertThat(optionalLens(Request(GET, "/")), absent())
    }
}
