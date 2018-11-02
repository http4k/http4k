package org.http4k.cloudnative.env


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.File

class EnvironmentTest {

    @Test
    fun `load from resource`() {
        val finalEnv = Environment.fromResource("local.properties") overrides Environment.from("FIRST" to "bill")

        assertThat(finalEnv["first"], equalTo("bob"))
    }

    @Test
    fun `load from file`() {
        val file = File("src/test/resources/local.properties")
        val finalEnv = Environment.fromFile(file) overrides Environment.from("FOO" to "bill")

        assertThat(finalEnv["first"], equalTo("bob"))
    }

    @Test
    fun overriding() {
        val finalEnv = Environment.from("FOO" to "bob") overrides Environment.from("FOO" to "bill")

        assertThat(finalEnv["FOO"], equalTo("bob"))
    }

    @Test
    fun defaults() {
        val default = EnvironmentKey.required("BAR")
        val finalEnv = Environment.from("FOO" to "bob") overrides Environment.defaults(default of "bill")

        assertThat(finalEnv["FOO"], equalTo("bob"))
        assertThat(default(finalEnv), equalTo("bill"))
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