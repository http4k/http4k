package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.with
import org.http4k.lens.MultipartForm
import org.http4k.lens.MultipartFormField
import org.junit.jupiter.api.Test

data class Foo(val bar: String)

class MultipartExtensionsTest {

    @Test
    fun `can inject into form with auto lens`() {
        val foo = Foo("bar")
        assertThat(
            MultipartForm()
                .with(MultipartFormField.auto<Foo>(Moshi).required("foo") of foo)
                .fields["foo"]!!.first().value,
            equalTo(Moshi.asFormatString(foo))
        )
    }

    @Test
    fun `can inject into form with json lens`() {
        val foo = Foo("bar")
        assertThat(
            MultipartForm()
                .with(MultipartFormField.json(Moshi).required("foo") of
                    Moshi { obj("bar" to string("bar")) })
                .fields["foo"]!!.first().value,
            equalTo(Moshi.asFormatString(foo))
        )
    }

}
