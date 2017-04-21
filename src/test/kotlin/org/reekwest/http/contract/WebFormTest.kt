package org.reekwest.http.contract

import org.reekwest.http.core.get

class WebFormTest {

    private val emptyRequest = get("")

//    @Test
//    fun `web form blows up if not URL content type`() {
//        val request = emptyRequest.copy(
//            headers = listOf("Content-Type" to "unknown"),
//            body = "hello=world&another=123".toBody())
//
//        assertThat({
//            Body.webForm(Strict,
//                FormField.required("hello"),
//                FormField.int().required("another")
//            )(request)
//        }, throws(equalTo(Invalid(CONTENT_TYPE))))
//    }
//
//    @Test
//    fun `web form extracts ok form values`() {
//        val request = emptyRequest.copy(
//            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
//            body = "hello=world&another=123".toBody())
//
//        val expected = mapOf("hello" to listOf("world"), "another" to listOf("123"))
//
//        assertThat(Body.webForm(Strict,
//            FormField.required("hello"),
//            FormField.int().required("another")
//        )(request), equalTo(WebForm(expected, emptyList())))
//    }
//
//    @Test
//    fun `feedback web form extracts ok form values and errors`() {
//        val request = emptyRequest.copy(
//            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
//            body = "another=123".toBody())
//
//        val requiredString = FormField.required("hello")
//        assertThat(Body.webForm(Feedback,
//            requiredString,
//            FormField.int().required("another")
//        )(request), equalTo(WebForm(mapOf("another" to listOf("123")), listOf(Missing(requiredString.meta)))))
//    }
//
//    @Test
//    fun `strict web form blows up with invalid form values`() {
//        val request = emptyRequest.copy(
//            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
//            body = "another=notANumber".toBody())
//
//        val stringRequiredField = FormField.required("hello")
//        val intRequiredField = FormField.int().required("another")
//        assertThat(
//            { Body.webForm(Strict, stringRequiredField, intRequiredField)(request) },
//            throws(equalTo(ContractBreach(Missing(stringRequiredField.meta), Invalid(intRequiredField.meta))))
//        )
//    }
}


