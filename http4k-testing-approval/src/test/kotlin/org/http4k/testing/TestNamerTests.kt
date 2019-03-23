package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class TestNamerTests {

    @Test
    fun `names files correctly`() {
        assertThat(TestNamer.Simple.nameFor(javaClass, javaClass.getMethod("names files correctly")), equalTo("TestNamerTests.names files correctly"))
    }
}