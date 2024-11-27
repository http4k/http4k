package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.TestNamer.Companion.ClassAndMethod
import org.http4k.testing.TestNamer.Companion.ClassDirAndMethod
import org.http4k.testing.TestNamer.Companion.MethodOnly
import org.junit.jupiter.api.Test

class TestNamerTests {

    @Test
    fun `method only`() {
        assertThat(
            MethodOnly.nameFor(javaClass, javaClass.getMethod("method only")),
            equalTo("org/http4k/testing/method only")
        )
    }

    @Test
    fun `class and method`() {
        assertThat(
            ClassAndMethod.nameFor(javaClass, javaClass.getMethod("class and method")),
            equalTo("org/http4k/testing/TestNamerTests.class and method")
        )
    }

    @Test
    fun `class dir and method`() {
        assertThat(
            ClassDirAndMethod.nameFor(javaClass, javaClass.getMethod("class dir and method")),
            equalTo("org/http4k/testing/TestNamerTests/class dir and method")
        )
    }
}
