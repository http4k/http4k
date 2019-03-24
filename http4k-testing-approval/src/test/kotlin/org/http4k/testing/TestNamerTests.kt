package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class TestNamerTests {

    @Test
    fun `class and method`() {
        assertThat(
            TestNamer.ClassAndMethod.nameFor(javaClass, javaClass.getMethod("class and method")),
            equalTo("org/http4k/testing/TestNamerTests.class and method")
        )
    }
}