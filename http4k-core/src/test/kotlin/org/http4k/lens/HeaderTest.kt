package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Accept
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_PDF
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.MULTIPART_FORM_DATA
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Parameter
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
    fun `value replaced`() {
        val single = Header.required("world")
        assertThat(single("value2", single("value1", request)), equalTo(request.header("world", "value2")))

        val multi = Header.multi.required("world")
        assertThat(multi(listOf("value3", "value4"), multi(listOf("value1", "value2"), request)),
            equalTo(request.header("world", "value3").header("world", "value4")))
    }

    @Test
    fun `value missing`() {
        assertThat(Header.optional("world")(request), absent())
        val requiredHeader = Header.required("world")
        assertThat({ requiredHeader(request) }, throws(lensFailureWith<Request>(Missing(requiredHeader.meta), overallType = Failure.Type.Missing)))

        assertThat(Header.defaulted("world", "bob")(request), equalTo("bob"))
        val defaultedHeader = Header.defaulted("world", "bob")
        assertThat(defaultedHeader(request), equalTo("bob"))

        assertThat(Header.multi.optional("world")(request), absent())
        val optionalMultiHeader = Header.multi.required("world")
        assertThat({ optionalMultiHeader(request) }, throws(lensFailureWith<Request>(Missing(optionalMultiHeader.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `invalid value`() {
        val requiredHeader = Header.map(String::toInt).required("hello")
        assertThat({ requiredHeader(request) }, throws(lensFailureWith<Request>(Invalid(requiredHeader.meta), overallType = Failure.Type.Invalid)))

        val optionalHeader = Header.map(String::toInt).optional("hello")
        assertThat({ optionalHeader(request) }, throws(lensFailureWith<Request>(Invalid(optionalHeader.meta), overallType = Failure.Type.Invalid)))

        val requiredMultiHeader = Header.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiHeader(request) }, throws(lensFailureWith<Request>(Invalid(requiredMultiHeader.meta), overallType = Failure.Type.Invalid)))

        val optionalMultiHeader = Header.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiHeader(request) }, throws(lensFailureWith<Request>(Invalid(optionalMultiHeader.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `sets value on request`() {
        val header = Header.required("bob")
        val withHeader = request.with(header of "hello")
        assertThat(header(withHeader), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Header.map(::MyCustomType, MyCustomType::value).required("bob")

        val instance = MyCustomType("hello world!")
        val reqWithHeader = custom(instance, Request(GET, ""))

        assertThat(reqWithHeader.header("bob"), equalTo("hello world!"))

        assertThat(custom(reqWithHeader), equalTo(MyCustomType("hello world!")))
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
        val lens = Header.CONTENT_TYPE
        val reqWithHeader = Request(GET, "").with(lens of ContentType.TEXT_HTML)
        assertThat(reqWithHeader.header("Content-Type"), equalTo("text/html; charset=utf-8"))
        assertThat(lens(reqWithHeader), equalTo(ContentType.TEXT_HTML))
    }

    @Test
    fun `content type serialises and deserialises correctly to message - with illegal directive is ignored`() {
        val lens = Header.CONTENT_TYPE
        val reqWithHeader = Request(GET, "").header("Content-Type", "bob; charset=UTF-8 ;boundary=asd; foomanchu; media-type=a/b")
        assertThat(lens(reqWithHeader), equalTo(ContentType("bob",
            listOf("charset" to "UTF-8", "boundary" to "asd", "media-type" to "a/b"))))
    }

    @Test
    fun `content type serialises and deserialises correctly to message - no directive`() {
        val lens = Header.CONTENT_TYPE
        val reqWithHeader = Request(GET, "").with(lens of ContentType("value"))
        assertThat(reqWithHeader.header("Content-Type"), equalTo("value"))
        assertThat(lens(reqWithHeader), equalTo(ContentType("value")))
    }

    @Test
    fun `accept header deserialises correctly from message`() {
        val accept = Header.ACCEPT(Request(GET, "").header("Accept", "text/html, application/pdf, application/xml;q=0.9, image/webp, */*;q=0.8"))!!

        assertThat(accept, equalTo(Accept(listOf(TEXT_HTML.withNoDirectives(), APPLICATION_PDF.withNoDirectives(), APPLICATION_XML.withNoDirectives()), listOf("q" to "0.9, image/webp, */*", "q" to "0.8"))))
        assertThat(accept.accepts(TEXT_HTML), equalTo(true))
        assertThat(accept.accepts(MULTIPART_FORM_DATA), equalTo(false))
    }

    @Test
    fun `multiple directives are parsed correctly`() {
        assertThat(Header.parseValueAndDirectives("some value"), equalTo("some value" to emptyList()))
        assertThat(Header.parseValueAndDirectives("some value ;"), equalTo("some value" to emptyList()))
        assertThat(Header.parseValueAndDirectives("some value; bob"), equalTo("some value" to listOf<Parameter>("bob" to null)))
        assertThat(Header.parseValueAndDirectives("some value; bob   ;bob2=anotherValue   "),
            equalTo("some value" to
                listOf("bob" to null, "bob2" to "anotherValue")))
    }

    @Test
    fun `enum`() {
        val requiredLens = Header.enum<Method>().required("method")
        assertThat(requiredLens(Request(GET, "/").header("method", "GET")), equalTo(GET))

        val optionalLens = Header.enum<Method>().optional("whatevs")
        assertThat(optionalLens(Request(GET, "/").header("whatevs", "GET")), equalTo(GET))
        assertThat(optionalLens(Request(GET, "/")), absent())
    }

    @Test
    fun `mapped enum`() {
        val requiredLens = Header.enum(MappedEnum::from, MappedEnum::to).required("whatevs")
        assertThat(requiredLens(Request(GET, "/").header("whatevs", "eulav")), equalTo(MappedEnum.value))
    }

    @Test
    fun `case-insensitive enum`() {
        val lens = Header.enum<Method>(caseSensitive = false).required("method")
        assertThat(lens(Request(GET, "/").header("method", "delete")), equalTo(Method.DELETE))
    }

    @Test
    fun `link header can get parsed and injected`() {
        val links = mapOf(
            "previous" to of("prevLink"),
            "next" to of("nextLink")
        )

        val link = Header.LINK

        val reqWithHeader = Request(GET, "").with(link of links)
        assertThat(reqWithHeader.header("Link"), equalTo("""<prevLink>; rel="previous", <nextLink>; rel="next""""))
        assertThat(link(reqWithHeader), equalTo(links))
    }

}
