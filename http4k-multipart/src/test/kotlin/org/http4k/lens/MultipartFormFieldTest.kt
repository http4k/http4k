package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class MultipartFormFieldTest {
    private val form = MultipartForm(mapOf("hello" to listOf("world", "world2")))

    @Test
    fun `value present`() {
        assertThat(MultipartFormField.optional("hello")(form), equalTo("world"))
        assertThat(MultipartFormField.required("hello")(form), equalTo("world"))
        assertThat(MultipartFormField.map { it.length }.required("hello")(form), equalTo(5))
        assertThat(MultipartFormField.map { it.length }.optional("hello")(form), equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(MultipartFormField.multi.required("hello")(form), equalTo(expected))
        assertThat(MultipartFormField.multi.optional("hello")(form), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(MultipartFormField.optional("world")(form), absent())
        val requiredFormField = MultipartFormField.required("world")
        assertThat({ requiredFormField(form) }, throws(lensFailureWith<MultipartForm>(Missing(requiredFormField.meta), overallType = Failure.Type.Missing)))

        assertThat(MultipartFormField.multi.optional("world")(form), absent())
        val optionalMultiFormField = MultipartFormField.multi.required("world")
        assertThat({ optionalMultiFormField(form) }, throws(lensFailureWith<MultipartForm>(Missing(optionalMultiFormField.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `value replaced`() {
        val single = MultipartFormField.required("world")
        assertThat(single("value2", single("value1", form)), equalTo(form + ("world" to "value2")))

        val multi = MultipartFormField.multi.required("world")
        assertThat(multi(listOf("value3", "value4"), multi(listOf("value1", "value2"), form)),
            equalTo(form + ("world" to "value3") + ("world" to "value4")))
    }

    @Test
    fun `invalid value`() {
        val requiredFormField = MultipartFormField.map(String::toInt).required("hello")
        assertThat({ requiredFormField(form) }, throws(lensFailureWith<MultipartForm>(Invalid(requiredFormField.meta), overallType = Failure.Type.Invalid)))

        val optionalFormField = MultipartFormField.map(String::toInt).optional("hello")
        assertThat({ optionalFormField(form) }, throws(lensFailureWith<MultipartForm>(Invalid(optionalFormField.meta), overallType = Failure.Type.Invalid)))

        val requiredMultiFormField = MultipartFormField.map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiFormField(form) }, throws(lensFailureWith<MultipartForm>(Invalid(requiredMultiFormField.meta), overallType = Failure.Type.Invalid)))

        val optionalMultiFormField = MultipartFormField.map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiFormField(form) }, throws(lensFailureWith<MultipartForm>(Invalid(optionalMultiFormField.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `sets value on form`() {
        val formField = MultipartFormField.required("bob")
        val withFormField = formField("hello", form)
        assertThat(formField(withFormField), equalTo("hello"))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = MultipartFormField.map(::MyCustomBodyType, { it.value }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val formWithField = custom(instance, MultipartForm())

        assertThat(formWithField.fields["bob"], equalTo(listOf("hello world!")))

        assertThat(custom(formWithField), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(MultipartFormField.required("hello").toString(), equalTo("Required form 'hello'"))
        assertThat(MultipartFormField.optional("hello").toString(), equalTo("Optional form 'hello'"))
        assertThat(MultipartFormField.multi.required("hello").toString(), equalTo("Required form 'hello'"))
        assertThat(MultipartFormField.multi.optional("hello").toString(), equalTo("Optional form 'hello'"))
    }
}