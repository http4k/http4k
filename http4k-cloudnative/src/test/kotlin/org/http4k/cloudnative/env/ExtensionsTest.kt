package org.http4k.cloudnative.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.File

class ExtensionsTest {

    @Test
    fun `can read yaml into properties`() {
        val env = Environment.fromYaml(File("src/test/resources/local.yaml"))

        assertThat(env["string"], equalTo("hello"))
        assertThat(env["child.string"], equalTo("world"))
        assertThat(env["child.child"], equalTo(""))
        assertThat(env["child.numbers"], equalTo("1,2"))
        assertThat(env["child.bool"], equalTo("true"))
        assertThat(env["numbers"], equalTo(""))
        assertThat(env["bool"], equalTo("false"))
    }
}
