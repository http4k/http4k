package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class SimpleTestNamerTest {

    @Test
    fun `names files correctly`() {
        assertThat(SimpleTestNamer().nameFor(javaClass, javaClass.getMethod("names files correctly")), equalTo("SimpleTestNamerTest.names files correctly"))
    }
}