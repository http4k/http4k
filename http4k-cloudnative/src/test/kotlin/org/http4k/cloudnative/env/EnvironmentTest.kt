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
}