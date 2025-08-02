package org.http4k.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ElementTest {

    @Test
    fun `trims whitespace and newlines on construction`() {
        assertThat(Element.of("  hello  \n goodbye   ").value, equalTo("hello   goodbye"))
    }
}
