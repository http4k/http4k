package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Validator.Feedback
import org.http4k.lens.Validator.Strict
import org.junit.jupiter.api.Test

class WebFormTest {

    private val emptyRequest = Request(GET, "")

    @Test
    fun `web form serialized into request`() {
        val stringField = FormField.required("hello")
        val intField = FormField.int().required("another")

        val webForm = Body.webForm(Strict, stringField, intField).toLens()

        val populatedRequest = emptyRequest.with(
            webForm of WebForm().with(stringField of "world", intField of 123)
        )

        assertThat(Header.CONTENT_TYPE(populatedRequest), equalTo(APPLICATION_FORM_URLENCODED))
        assertThat(populatedRequest.bodyString(), equalTo("hello=world&another=123"))
    }

    @Test
    fun `web form blows up if not URL content type`() {
        val request = emptyRequest.header("Content-Type", "unknown").body(Body("hello=world&another=123"))

        assertThat({
            Body.webForm(Strict,
                FormField.required("hello"),
                FormField.int().required("another")
            ).toLens()(request)
        }, throws(lensFailureWith<ContentType>(Unsupported(CONTENT_TYPE.meta), overallType = Failure.Type.Unsupported)))
    }

    @Test
    fun `web form extracts ok form values`() {
        val request = emptyRequest.header("Content-Type", APPLICATION_FORM_URLENCODED.value).body(Body("hello=world&another=123"))

        val expected = mapOf("hello" to listOf("world"), "another" to listOf("123"))

        assertThat(Body.webForm(Strict,
            FormField.required("hello"),
            FormField.int().required("another")
        ).toLens()(request), equalTo(WebForm(expected, emptyList())))
    }

    @Test
    fun `feedback web form extracts ok form values and errors`() {
        val request = emptyRequest.header("Content-Type", APPLICATION_FORM_URLENCODED.value).body(Body("another=123"))

        val requiredString = FormField.required("hello")
        assertThat(Body.webForm(Feedback,
            requiredString,
            FormField.int().required("another")
        ).toLens()(request), equalTo(WebForm(mapOf("another" to listOf("123")), listOf(Missing(requiredString.meta)))))
    }

    @Test
    fun `strict web form blows up with invalid form values`() {
        val request = emptyRequest.header("Content-Type", APPLICATION_FORM_URLENCODED.value).body(Body("another=notANumber"))

        val stringRequiredField = FormField.required("hello")
        val intRequiredField = FormField.int().required("another")
        assertThat(
            { Body.webForm(Strict, stringRequiredField, intRequiredField).toLens()(request) },
            throws(lensFailureWith<Any?>(Missing(stringRequiredField.meta), Invalid(intRequiredField.meta), overallType = Failure.Type.Invalid))
        )
    }

    @Test
    fun `can set multiple values on a form`() {
        val stringField = FormField.required("hello")
        val intField = FormField.int().required("another")

        val populated = WebForm()
            .with(stringField of "world",
                intField of 123)

        assertThat(stringField(populated), equalTo("world"))
        assertThat(intField(populated), equalTo(123))
    }
}
