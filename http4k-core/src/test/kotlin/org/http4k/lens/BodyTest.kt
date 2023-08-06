package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.ParamMeta.StringParam
import org.junit.jupiter.api.Test

class BodyTest {

    private val emptyRequest = Request(GET, "")

    @Test
    fun `can get string body when lax`() {
        val laxContentType = Body.string(TEXT_PLAIN).toLens()
        assertThat(laxContentType(emptyRequest.body("some value")), equalTo("some value"))
        assertThat(laxContentType(emptyRequest.header("Content-type", TEXT_PLAIN.toHeaderValue()).body("some value")), equalTo("some value"))
    }

    @Test
    fun `can get regex body`() {
        val regexBody = Body.regex("bob(.+)alice").toLens()
        assertThat(regexBody(emptyRequest.body("bobritaalice")), equalTo("rita"))
        assertThat({ regexBody(emptyRequest.body("foobaralice")) }, throws(lensFailureWith<Request>(Invalid(Meta(true, "body", StringParam, "body")), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `non empty string`() {
        val nonEmpty = Body.nonEmptyString(TEXT_PLAIN).toLens()
        assertThat(nonEmpty(emptyRequest.body("123")), equalTo("123"))
        assertThat({ nonEmpty(emptyRequest.body("")) }, throws(lensFailureWith<Request>(Invalid(Meta(true, "body", StringParam, "body")), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `non blank string`() {
        val nonEmpty = Body.nonBlankString(TEXT_PLAIN).toLens()
        assertThat(nonEmpty(emptyRequest.body("123")), equalTo("123"))
        assertThat({ nonEmpty(emptyRequest.body("")) }, throws(lensFailureWith<Request>(Invalid(Meta(true, "body", StringParam, "body")), overallType = Failure.Type.Invalid)))
        assertThat({ nonEmpty(emptyRequest.body(" ")) }, throws(lensFailureWith<Request>(Invalid(Meta(true, "body", StringParam, "body")), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `rejects invalid or missing content type when ContentNegotiation Strict`() {
        val strictBody = Body.string(TEXT_PLAIN, contentNegotiation = ContentNegotiation.Strict).toLens()
        assertThat({ strictBody(emptyRequest.body("some value")) }, throws(lensFailureWith<Any?>(Unsupported(CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
        assertThat({ strictBody(emptyRequest.header("content-type", "text/bob;charset=not-utf-8").body("some value")) }, throws(lensFailureWith<ContentType>(Unsupported(CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
    }

    @Test
    fun `rejects invalid or missing content type when ContentNegotiation StrictNoDirective`() {
        val strictNoDirectiveBody = Body.string(TEXT_PLAIN, contentNegotiation = ContentNegotiation.StrictNoDirective).toLens()
        assertThat({ strictNoDirectiveBody(emptyRequest.body("some value")) }, throws(lensFailureWith<Any?>(Unsupported(CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
        assertThat(strictNoDirectiveBody(emptyRequest.header("content-type", "text/plain;  charset= not-utf-8  ").body("some value")), equalTo("some value"))
    }

    @Test
    fun `rejects invalid content type when ContentNegotiation NonStrict`() {
        val strictBody = Body.string(TEXT_PLAIN, contentNegotiation = ContentNegotiation.NonStrict).toLens()
        assertThat({ strictBody(emptyRequest.header("content-type", "text/bob;  charset= not-utf-8  ").body("some value")) }, throws(lensFailureWith<ContentType>(Unsupported(CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
        assertThat(strictBody(emptyRequest.body("some value")), equalTo("some value"))
    }

    @Test
    fun `accept any content type when ContentNegotiation None`() {
        val noneBody = Body.string(TEXT_PLAIN, contentNegotiation = ContentNegotiation.None).toLens()
        noneBody(emptyRequest.body("some value"))
        noneBody(emptyRequest.body("some value").header("content-type", "text/bob"))
    }

    @Test
    fun `sets value on request`() {
        val body = Body.string(TEXT_PLAIN).toLens()
        val withBody = emptyRequest.with(body of "hello")
        assertThat(body(withBody), equalTo("hello"))
        assertThat(CONTENT_TYPE(withBody), equalTo(TEXT_PLAIN))
    }

    @Test
    fun `synonym methods roundtrip`() {
        val body = Body.string(TEXT_PLAIN).toLens()
        body.inject("hello", emptyRequest)
        val withBody = emptyRequest.with(body of "hello")
        assertThat(body.extract(withBody), equalTo("hello"))
    }

    @Test
    fun `can create a custom Body type and get and set on request`() {
        val customBody = Body.string(TEXT_PLAIN).map(::MyCustomType, MyCustomType::value).toLens()

        val custom = MyCustomType("hello world!")
        val reqWithBody = customBody(custom, emptyRequest)

        assertThat(reqWithBody.bodyString(), equalTo("hello world!"))

        assertThat(customBody(reqWithBody), equalTo(MyCustomType("hello world!")))
    }

    @Test
    fun `can create a one way custom Body type`() {
        val customBody = Body.string(TEXT_PLAIN).map(::MyCustomType).toLens()
        assertThat(customBody(emptyRequest
            .header("Content-type", TEXT_PLAIN.toHeaderValue())
            .body("hello world!")), equalTo(MyCustomType("hello world!")))
    }
}
