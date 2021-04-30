package org.http4k.cloudnative.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException

class EnvironmentTest {

    @Test
    fun `load from resource`() {
        val finalEnv = Environment.fromResource("local.properties") overrides Environment.from("FIRST" to "bill")

        assertThat(finalEnv["first"], equalTo("bob"))

        assertThat({ Environment.fromResource("notthere.properties") }, throws<FileNotFoundException>())
    }

    @Test
    fun `load from file`() {
        val file = File("src/test/resources/local.properties")
        val finalEnv = Environment.from(file) overrides Environment.from("FOO" to "bill")

        assertThat(finalEnv["first"], equalTo("bob"))

        assertThat({ Environment.from(File("foobar")) }, throws<FileNotFoundException>())
    }

    @Test
    fun overriding() {
        val finalEnv = Environment.from("FOO" to "bob") overrides Environment.from("FOO" to "bill")

        assertThat(finalEnv["FOO"], equalTo("bob"))
    }

    @Test
    fun `overriding overrides separator`() {
        val finalEnv = MapEnvironment.from(listOf("FOO" to "foo;bar").toMap().toProperties(), separator = ";") overrides Environment.from("FOO" to "bob")

        assertThat(EnvironmentKey.required("FOO")[finalEnv], equalTo("foo"))
    }

    @Test
    fun `add to overriding environment`() {
        val finalEnv = Environment.from("FOO" to "bob") overrides Environment.from("BAR" to "bill")
        val extendedEnv = finalEnv.set("BAZ", "bud")

        assertThat(extendedEnv["FOO"], equalTo("bob"))
        assertThat(extendedEnv["BAR"], equalTo("bill"))
        assertThat(extendedEnv["BAZ"], equalTo("bud"))
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
        infix fun String.shouldConvertTo(expected: String) = assertThat(this, convertFromKey(), equalTo(expected))

        "" shouldConvertTo ""
        "first" shouldConvertTo "first"
        "FIRST" shouldConvertTo "first"
        "firstName" shouldConvertTo "firstname"
        "FIRST-NAME" shouldConvertTo "first-name"
        "FIRST.NAME" shouldConvertTo "first-name"
    }
}
