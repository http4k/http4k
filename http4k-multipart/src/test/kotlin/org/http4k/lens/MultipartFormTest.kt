package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MultipartFormTest {

    private val emptyRequest = Request(Method.GET, "")
    private val stringRequiredField = MultipartFormField.required("hello")
    private val intRequiredField = MultipartFormField.int().required("another")
    private val requiredFile = MultipartFormFile.required("file")

    private val validBody = javaClass.getResourceAsStream("hello.txt").reader().readText()
    private fun validFile() = FormFile("hello.txt", ContentType.TEXT_HTML, "bits".byteInputStream())

    private val DEFAULT_BOUNDARY = "hello"
    private val CONTENT_TYPE_WITH_BOUNDARY = ContentType.MultipartFormWithBoundary(DEFAULT_BOUNDARY)

    @Test
    @Disabled
    fun `multipart form serialized into request`() {
        val populatedRequest = emptyRequest.with(
            multipartFormLens(Validator.Strict) of MultipartForm().with(
                stringRequiredField of "world",
                intRequiredField of 123,
                requiredFile of validFile()
            )
        )

        assertThat(Header.CONTENT_TYPE(populatedRequest), equalTo(CONTENT_TYPE_WITH_BOUNDARY))
        assertThat(populatedRequest.bodyString(), equalTo(validBody))
    }

    @Test
    fun `multipart form blows up if not correct content type`() {
        val request = emptyRequest.with(
            multipartFormLens(Validator.Strict) of MultipartForm().with(
                stringRequiredField of "world",
                intRequiredField of 123,
                requiredFile of validFile()
            )).replaceHeader("Content-Type", "unknown; boundary=hello")

        assertThat({
            multipartFormLens(Validator.Strict)(request)
        }, throws(lensFailureWith<MultipartForm>(Unsupported(Header.CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
    }

    @Test
    fun `multipart form extracts ok form values`() {
        val request = emptyRequest.with(
            multipartFormLens(Validator.Strict) of MultipartForm().with(
                stringRequiredField of "world",
                intRequiredField of 123,
                requiredFile of validFile()
            )
        )

        val expected = MultipartForm(
            mapOf("hello" to listOf("world"),
                "another" to listOf("123")),
            mapOf("file" to listOf(validFile())))
        assertThat(multipartFormLens(Validator.Strict)(request), equalTo(expected))
    }

    @Test
    fun `feedback multipart form extracts ok form values and errors`() {

        val request = emptyRequest.with(
            multipartFormLens(Validator.Feedback) of MultipartForm().with(
                intRequiredField of 123,
                requiredFile of validFile()
            )
        )

        val requiredString = MultipartFormField.required("hello")
        assertThat(multipartFormLens(Validator.Feedback)(request), equalTo(MultipartForm(
            mapOf("another" to listOf("123")),
            mapOf("file" to listOf(validFile())),
            listOf(Missing(requiredString.meta)))))
    }

    @Test
    fun `strict multipart form blows up with invalid form values`() {
        val intStringField = MultipartFormField.required("another")

        val request = emptyRequest.with(
            Body.multipartForm(Validator.Strict, stringRequiredField, intStringField, requiredFile, defaultBoundary = DEFAULT_BOUNDARY).toLens() of
                MultipartForm().with(
                    stringRequiredField of "hello",
                    intStringField of "world",
                    requiredFile of validFile()
                )
        )
        assertThat(
            { multipartFormLens(Validator.Strict)(request) },
                throws(lensFailureWith<MultipartForm>(Invalid(intRequiredField.meta), overallType = Failure.Type.Invalid))
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