package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class HasLensTest {
    data class Foo(val bar: String) {
        companion object : HasLens<Foo>(Jackson, kClass())
    }

    @Test
    fun `can use companion to provide an automatic lens to a class`() {
        val item = Foo("bar")
        val injected = Request(GET, "").with(Foo.lens of item)

        assertThat(injected.bodyString(), equalTo("""{"bar":"bar"}"""))
        assertThat(Foo.lens(injected), equalTo(item)) // returns Foo("bar"
    }
}
