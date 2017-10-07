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
    private val stringRequiredField = MultipartFormField.required("hello")
    private val intRequiredField = MultipartFormField.int().required("another")
    private val requiredFile = MultipartFormFile.required("file")

    private val validBody = String(javaClass.getResourceAsStream("hello.txt").readBytes())
    private val validFile = MultipartFormFile("hello.txt", ContentType.TEXT_HTML, "bits".byteInputStream())

    private val DEFAULT_BOUNDARY = "hello"
    private val CONTENT_TYPE_WITH_BOUNDARY = ContentType.MultipartFormWithBoundary(DEFAULT_BOUNDARY)

    @Test
    @Ignore
    fun `multipart form serialized into request`() {
        val populatedRequest = emptyRequest.with(
            multipartFormLens(Validator.Strict) of MultipartForm().with(
                stringRequiredField of "world",
                intRequiredField of 123,
                requiredFile of validFile
            )
        )

        assertThat(Header.Common.CONTENT_TYPE(populatedRequest), equalTo(CONTENT_TYPE_WITH_BOUNDARY))
        assertThat(populatedRequest.bodyString(), equalTo(validBody))
    }

    @Test
    @Ignore
    fun `multipart form blows up if not correct content type`() {
        val request = emptyRequest.header("Content-Type", "unknown; boundary=hello").body(validBody)

        assertThat({
            multipartFormLens(Validator.Strict)(request)
        }, throws(lensFailureWith(Unsupported(Header.Common.CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
    }

    @Test
    @Ignore
    fun `multipart form extracts ok form values`() {
        val request = emptyRequest.header("Content-Type", CONTENT_TYPE_WITH_BOUNDARY.value).body(validBody)

        val expected = mapOf("hello" to listOf("world"), "another" to listOf("123"))

        assertThat(multipartFormLens(Validator.Strict)(request), equalTo(MultipartForm(expected)))
    }

    @Test
    @Ignore
    fun `feedback multipart form extracts ok form values and errors`() {
        val request = emptyRequest.header("Content-Type", CONTENT_TYPE_WITH_BOUNDARY.value).body(validBody)

        val requiredString = MultipartFormField.required("hello")
        assertThat(multipartFormLens(Validator.Feedback)(request), equalTo(MultipartForm(mapOf("another" to listOf("123")), emptyMap(), listOf(Missing(requiredString.meta)))))
    }

    @Test
    @Ignore
    fun `strict multipart form blows up with invalid form values`() {
        val request = emptyRequest.header("Content-Type", CONTENT_TYPE_WITH_BOUNDARY.value).body("another=notANumber".toBody())

        val stringRequiredField = MultipartFormField.required("hello")
        val intRequiredField = MultipartFormField.int().required("another")
        assertThat(
            { multipartFormLens(Validator.Strict)(request) },
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

    private fun multipartFormLens(validator: Validator) = Body.multipartForm(validator, stringRequiredField, intRequiredField, requiredFile, defaultBoundary = DEFAULT_BOUNDARY).toLens()
}