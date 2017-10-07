package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.toBody
import org.http4k.core.with
import org.junit.Ignore
import org.junit.Test

class MultipartFormTest {

    private val emptyRequest = Request(Method.GET, "")
    private val validBody = String(javaClass.getResourceAsStream("hello.txt").readBytes())

    @Test
    fun `multipart form serialized into request`() {
        val stringField = MultipartFormField.required("hello")
        val intField = MultipartFormField.int().required("another")
        val aFile = MultipartFormFile.required("file")

        val multipartForm = Body.multipartForm(Validator.Strict, stringField, intField, aFile, boundary = "hello").toLens()

        val populatedRequest = emptyRequest.with(
            multipartForm of MultipartForm().with(
                stringField of "world", intField of 123,
                aFile of MultipartFormFile("hello.txt", ContentType.TEXT_HTML, "bits".byteInputStream())
            )
        )

        println(populatedRequest)
        assertThat(Header.Common.CONTENT_TYPE(populatedRequest), equalTo(ContentType.MultipartFormWithBoundary("hello")))
        assertThat(populatedRequest.bodyString(), equalTo(validBody))
    }

    @Test
    fun `multipart form blows up if not correct content type`() {
        val request = emptyRequest.header("Content-Type", "unknown; boundary=hello").body(validBody)

        assertThat({
            Body.multipartForm(Validator.Strict, MultipartFormField.required("hello")).toLens()(request)
        }, throws(lensFailureWith(Unsupported(Header.Common.CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
    }

    @Test
    @Ignore
    fun `multipart form extracts ok form values`() {
        val request = emptyRequest.header("Content-Type", ContentType.MultipartFormWithBoundary("hello").value).body(validBody)

        println(request)
        val expected = mapOf("hello" to listOf("world"), "another" to listOf("123"))

        val stringField = MultipartFormField.required("hello")
        val intField = MultipartFormField.int().required("another")
        val aFile = MultipartFormFile.required("file")

        assertThat(Body.multipartForm(Validator.Strict,stringField, intField, aFile).toLens()(request), equalTo(MultipartForm(expected)))
    }

    @Test
    @Ignore
    fun `feedback multipart form extracts ok form values and errors`() {
        val request = emptyRequest.header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.value).body("another=123".toBody())

        val requiredString = MultipartFormField.required("hello")
        assertThat(Body.multipartForm(Validator.Feedback,
            requiredString,
            MultipartFormField.int().required("another")
        ).toLens()(request), equalTo(MultipartForm(mapOf("another" to listOf("123")), emptyMap(), listOf(Missing(requiredString.meta)))))
    }

    @Test
    @Ignore
    fun `strict multipart form blows up with invalid form values`() {
        val request = emptyRequest.header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.value).body("another=notANumber".toBody())

        val stringRequiredField = MultipartFormField.required("hello")
        val intRequiredField = MultipartFormField.int().required("another")
        assertThat(
            { Body.multipartForm(Validator.Strict, stringRequiredField, intRequiredField).toLens()(request) },
            throws(lensFailureWith(Missing(stringRequiredField.meta), Invalid(intRequiredField.meta), overallType = Failure.Type.Invalid))
        )
    }

    @Test
    fun `can set multiple values on a form`() {
        val stringField = MultipartFormField.required("hello")
        val intField = MultipartFormField.int().required("another")

        val populated = MultipartForm()
            .with(stringField of "world",
                intField of 123)

        assertThat(stringField(populated), equalTo("world"))
        assertThat(intField(populated), equalTo(123))
    }
}