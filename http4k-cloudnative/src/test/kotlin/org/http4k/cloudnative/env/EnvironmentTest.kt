package org.http4k.cloudnative.env


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class EnvironmentTest {

    @Test
    fun overriding() {
        val finalEnv = Environment.from("FOO" to "bob") overrides Environment.from("FOO" to "bill")

        assertThat(finalEnv["FOO"], equalTo("bob"))
    }

    @Test
    fun `convert keys`() {
        infix fun String.shouldConvertTo(expected: String) = assertThat(convertToKey(), equalTo(expected))

        "" shouldConvertTo ""
        "first" shouldConvertTo "FIRST"
        "firstName" shouldConvertTo "FIRST_NAME"
        "first-name" shouldConvertTo "FIRST_NAME"
        "first.name" shouldConvertTo "FIRST_NAME"
    }
}