package org.http4k.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class FragmentTest {

    @Test
    fun `trims whitespace on construction`() {
        assertThat(Fragment.of("  hello  ").value, equalTo("hello"))
    }
}
