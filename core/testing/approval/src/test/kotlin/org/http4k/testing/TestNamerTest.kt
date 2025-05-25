package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.TestNamer.Companion.ClassAndMethod
import org.http4k.testing.TestNamer.Companion.ClassDirAndMethod
import org.http4k.testing.TestNamer.Companion.MethodOnly
import org.junit.jupiter.api.Test

class TestNamerTest {

    @Test
    fun `method only`() = runBlocking {
        assertThat(
            MethodOnly.nameFor(javaClass, javaClass.getMethod("method only")),
            equalTo("org/http4k/testing/method only")
        )
    }

    @Test
    fun `class and method`() = runBlocking {
        assertThat(
            ClassAndMethod.nameFor(javaClass, javaClass.getMethod("class and method")),
            equalTo("org/http4k/testing/TestNamerTest.class and method")
        )
    }

    @Test
    fun `class dir and method`() = runBlocking {
        assertThat(
            ClassDirAndMethod.nameFor(javaClass, javaClass.getMethod("class dir and method")),
            equalTo("org/http4k/testing/TestNamerTest/class dir and method")
        )
    }
}
