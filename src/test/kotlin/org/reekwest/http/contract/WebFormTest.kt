package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.contract.FormValidator.Feedback
import org.reekwest.http.contract.FormValidator.Strict
import org.reekwest.http.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.contract.WebForm.Companion.emptyForm
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.get
import org.reekwest.http.core.with

class WebFormTest {

    private val emptyRequest = get("")

    @Test
    fun `web form serialized into request`() {
        val stringField = FormField.required("hello")
        val intField = FormField.int().required("another")

        val webForm = Body.webForm(Strict, stringField, intField)

        val populatedRequest = emptyRequest.with(
            webForm to emptyForm().with(stringField to "world", intField to 123)
        )

        assertThat(CONTENT_TYPE(populatedRequest), equalTo(APPLICATION_FORM_URLENCODED))
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
        }, throws(equalTo(ContractBreach(Invalid(CONTENT_TYPE)))))
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
            throws(equalTo(ContractBreach(Missing(stringRequiredField.meta), Invalid(intRequiredField.meta))))
        )
    }

    @Test
    fun `can set multiple values on a form`() {
        val stringField = FormField.required("hello")
        val intField = FormField.int().required("another")

        val populated = WebForm.emptyForm()
            .with(stringField to "world",
                intField to 123)

        assertThat(stringField(populated), equalTo("world"))
        assertThat(intField(populated), equalTo(123))
    }
}


