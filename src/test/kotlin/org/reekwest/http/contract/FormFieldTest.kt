package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

class FormFieldTest {
    private val form = WebForm(mapOf("hello" to listOf("world", "world2")), emptyList())

    private fun withFormFieldOf(s: String) = WebForm(mapOf("hello" to listOf(s)), listOf())

    @Test
    fun `value present`() {
        assertThat(FormField.optional("hello")(form), equalTo("world"))
        assertThat(FormField.required("hello")(form), equalTo("world"))
        assertThat(FormField.map { it.length }.required("hello")(form), equalTo(5))
        assertThat(FormField.map { it.length }.optional("hello")(form), equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(FormField.multi.required("hello")(form), equalTo(expected))
        assertThat(FormField.multi.optional("hello")(form), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(FormField.optional("world")(form), absent())
        val requiredFormField = FormField.required("world")
        assertThat({ requiredFormField(form) }, throws(equalTo(ContractBreach(Missing(requiredFormField)))))

        assertThat(FormField.multi.optional("world")(form), absent())
        val optionalMultiFormField = FormField.multi.required("world")
        assertThat({ optionalMultiFormField(form) }, throws(equalTo(ContractBreach(Missing(optionalMultiFormField)))))
    }

    @Test
    fun `invalid value`() {
        val requiredFormField = FormField.map(String::toInt).required("hello")
        assertThat({ requiredFormField(form) }, throws(equalTo(ContractBreach(Invalid(requiredFormField)))))

        val optionalFormField = FormField.map(String::toInt).optional("hello")
        assertThat({ optionalFormField(form) }, throws(equalTo(ContractBreach(Invalid(optionalFormField)))))

        val requiredMultiFormField = FormField.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiFormField(form) }, throws(equalTo(ContractBreach(Invalid(requiredMultiFormField)))))

        val optionalMultiFormField = FormField.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiFormField(form) }, throws(equalTo(ContractBreach(Invalid(optionalMultiFormField)))))
    }

    @Test
    fun `int`() {
        val optionalFormField = FormField.int().optional("hello")
        val formWithFormField = withFormFieldOf("123")
        assertThat(optionalFormField(formWithFormField), equalTo(123))

        assertThat(FormField.int().optional("world")(withFormFieldOf("/")), absent())

        val badForm = withFormFieldOf("/?hello=notAnumber")
        assertThat({ optionalFormField(badForm) }, throws(equalTo(ContractBreach(Invalid(optionalFormField)))))
    }

    @Test
    fun `sets value on form`() {
        val formField = FormField.required("bob")
        val withFormField = formField("hello", form)
        assertThat(formField(withFormField), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = FormField.map({ MyCustomBodyType(it) }, { it.value }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val formWithField = custom(instance, WebForm.emptyForm())

        assertThat(formWithField.fields["bob"], equalTo(listOf("hello world!")))

        assertThat(custom(formWithField), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(FormField.required("hello").toString(), equalTo("Required form field 'hello'"))
        assertThat(FormField.optional("hello").toString(), equalTo("Optional form field 'hello'"))
        assertThat(FormField.multi.required("hello").toString(), equalTo("Required form field 'hello'"))
        assertThat(FormField.multi.optional("hello").toString(), equalTo("Optional form field 'hello'"))
    }
}