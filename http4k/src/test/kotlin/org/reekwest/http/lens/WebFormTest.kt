package org.http4k.http.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.http4k.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.http.core.toBody
import org.http4k.http.core.with
import org.http4k.http.lens.FormValidator.Feedback
import org.http4k.http.lens.FormValidator.Strict
import org.http4k.http.lens.Header.Common.CONTENT_TYPE

class WebFormTest {

    private val emptyRequest = get("")

    @Test
    fun `web form serialized into request`() {
        val stringField = FormField.required("hello")
        val intField = FormField.int().required("another")

        val webForm = Body.webForm(Strict, stringField, intField)

        val populatedRequest = emptyRequest.with(
            webForm to WebForm().with(stringField to "world", intField to 123)
        )

        assertThat(Header.Common.CONTENT_TYPE(populatedRequest), equalTo(APPLICATION_FORM_URLENCODED))
        assertThat(populatedRequest.bodyString(), equalTo("hello=world&another=123"))
    }

    @Test
    fun `web form blows up if not URL content type`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to "unknown"),
            body = "hello=world&another=123".toBody())

        assertThat({
            Body.webForm(Strict,
                FormField.required("hello"),
                FormField.int().required("another")
            )(request)
        }, throws(equalTo(LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE))))
    }

    @Test
    fun `web form extracts ok form values`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
            body = "hello=world&another=123".toBody())

        val expected = mapOf("hello" to listOf("world"), "another" to listOf("123"))

        assertThat(Body.webForm(Strict,
            FormField.required("hello"),
            FormField.int().required("another")
        )(request), equalTo(WebForm(expected, emptyList())))
    }

    @Test
    fun `feedback web form extracts ok form values and errors`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
            body = "another=123".toBody())

        val requiredString = FormField.required("hello")
        assertThat(Body.webForm(Feedback,
            requiredString,
            FormField.int().required("another")
        )(request), equalTo(WebForm(mapOf("another" to listOf("123")), listOf(Missing(requiredString.meta)))))
    }

    @Test
    fun `strict web form blows up with invalid form values`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
            body = "another=notANumber".toBody())

        val stringRequiredField = FormField.required("hello")
        val intRequiredField = FormField.int().required("another")
        assertThat(
            { Body.webForm(Strict, stringRequiredField, intRequiredField)(request) },
            throws(equalTo(LensFailure(Missing(stringRequiredField.meta), Invalid(intRequiredField.meta), status = NOT_ACCEPTABLE)))
        )
    }

    @Test
    fun `can set multiple values on a form`() {
        val stringField = FormField.required("hello")
        val intField = FormField.int().required("another")

        val populated = WebForm()
            .with(stringField to "world",
                intField to 123)

        assertThat(stringField(populated), equalTo("world"))
        assertThat(intField(populated), equalTo(123))
    }
}


