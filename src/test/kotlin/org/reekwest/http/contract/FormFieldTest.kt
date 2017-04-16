package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.contract.ContractBreach.Companion.Invalid
import org.reekwest.http.core.contract.ContractBreach.Companion.Missing
import org.reekwest.http.core.contract.FormField
import org.reekwest.http.core.contract.WebForm
import org.reekwest.http.core.contract.int

class FormFieldTest {
    private val request = WebForm(mapOf("hello" to listOf("world", "world2")), emptyList())

    private fun withFormFieldOf(s: String) = WebForm(mapOf("hello" to listOf(s)), listOf())

    @Test
    fun `value present`() {
        assertThat(FormField.optional("hello")(request), equalTo("world"))
        assertThat(FormField.required("hello")(request), equalTo("world"))
        assertThat(FormField.map { it.length }.required("hello")(request), equalTo(5))
        assertThat(FormField.map { it.length }.optional("hello")(request), equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(FormField.multi.required("hello")(request), equalTo(expected))
        assertThat(FormField.multi.optional("hello")(request), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(FormField.optional("world")(request), absent())
        val requiredFormField = FormField.required("world")
        assertThat({ requiredFormField(request) }, throws(equalTo(Missing(requiredFormField))))

        assertThat(FormField.multi.optional("world")(request), equalTo(emptyList()))
        val optionalMultiFormField = FormField.multi.required("world")
        assertThat({ optionalMultiFormField(request) }, throws(equalTo(Missing(optionalMultiFormField))))
    }

    @Test
    fun `invalid value`() {
        val requiredFormField = FormField.map(String::toInt).required("hello")
        assertThat({ requiredFormField(request) }, throws(equalTo(Invalid(requiredFormField))))

        val optionalFormField = FormField.map(String::toInt).optional("hello")
        assertThat({ optionalFormField(request) }, throws(equalTo(Invalid(optionalFormField))))

        val requiredMultiFormField = FormField.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiFormField(request) }, throws(equalTo(Invalid(requiredMultiFormField))))

        val optionalMultiFormField = FormField.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiFormField(request) }, throws(equalTo(Invalid(optionalMultiFormField))))
    }

    @Test
    fun `int`() {
        val optionalFormField = FormField.int().optional("hello")
        val requestWithFormField = withFormFieldOf("123")
        assertThat(optionalFormField(requestWithFormField), equalTo(123))

        assertThat(FormField.int().optional("world")(withFormFieldOf("/")), absent())

        val badRequest = withFormFieldOf("/?hello=notAnumber")
        assertThat({ optionalFormField(badRequest) }, throws(equalTo(Invalid(optionalFormField))))
    }
//
//    @Test
//    fun `sets value on request`() {
//        val formField = FormField.required("bob")
//        val withFormField = formField("hello", request)
//        assertThat(formField(withFormField), equalTo("hello"))
//    }
//
//    @Test
//    fun `can create a custom type and get and set on request`() {
//        val custom = FormField.map({ MyCustomBodyType(it) }, { it.value }).required("bob")
//
//        val instance = MyCustomBodyType("hello world!")
//        val reqWithFormField = custom(instance, get(""))
//
//        assertThat(reqWithFormField.formField("bob"), equalTo("hello world!"))
//
//        assertThat(custom(reqWithFormField), equalTo(MyCustomBodyType("hello world!")))
//    }
//
//    @Test
//    fun `toString is ok`() {
//        assertThat(FormField.required("hello").toString(), equalTo("Required formField 'hello'"))
//        assertThat(FormField.optional("hello").toString(), equalTo("Optional formField 'hello'"))
//        assertThat(FormField.multi.required("hello").toString(), equalTo("Required formField 'hello'"))
//        assertThat(FormField.multi.optional("hello").toString(), equalTo("Optional formField 'hello'"))
//    }
}