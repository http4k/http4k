package org.http4k.config

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.LensFailure
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.long
import org.http4k.lens.of
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Properties

class EnvironmentKeyTest {

    private val env = Environment.EMPTY

    @Test
    fun `custom key roundtrip`() {

        val lens = EnvironmentKey.int().required("some-value")
        assertThrows<LensFailure> { lens(env) }

        val withInjectedValue = env.with(lens of 80)
        assertThat(lens(withInjectedValue), equalTo(80))

        assertThat(withInjectedValue["SOME_VALUE"], equalTo("80"))

        assertThat(EnvironmentKey.int().required("SOME_VALUE")(withInjectedValue), equalTo(80))
    }

    @Test
    fun `custom multi key roundtrip`() {
        val lens = EnvironmentKey.int().multi.required("some-value")
        assertThrows<LensFailure> { lens(env) }

        val withInjectedValue = env.with(lens of listOf(80, 81))

        assertThat(withInjectedValue["SOME_VALUE"], equalTo("80,81"))

        assertThat(EnvironmentKey.int().multi.required("SOME_VALUE")(withInjectedValue), equalTo(listOf(80, 81)))
        assertThat(
            EnvironmentKey.int().multi.required("SOME_VALUE")(Environment.from("SOME_VALUE" to "80  , 81  ")),
            equalTo(listOf(80, 81))
        )
    }

    @Test
    fun `custom multi key roundtrip with non-standard separator`() {
        val customEnv = MapEnvironment.from(Properties(), separator = ";")
        val lens = EnvironmentKey.int().multi.required("some-value")
        assertThrows<LensFailure> { lens(customEnv) }

        val withInjectedValue = customEnv.with(lens of listOf(80, 81))

        assertThat(withInjectedValue["SOME_VALUE"], equalTo("80;81"))

        assertThat(EnvironmentKey.int().multi.required("SOME_VALUE")(withInjectedValue), equalTo(listOf(80, 81)))
        assertThat(
            EnvironmentKey.int().multi.required("SOME_VALUE")(
                MapEnvironment.from(
                    listOf("SOME_VALUE" to "80  ; 81  ").toMap().toProperties(), separator = ";"
                )
            ), equalTo(listOf(80, 81))
        )
    }

    @Test
    fun `value replaced`() {
        val single = EnvironmentKey.int().required("value")

        val original = env.with(EnvironmentKey.k8s.HEALTH_PORT of 81)
        assertThat(single(single(2, single(1, original))), equalTo(2))

        val multi = EnvironmentKey.int().multi.required("value")
        assertThat(
            multi(multi(listOf(3, 4), multi(listOf(1, 2), original))),
            equalTo(listOf(3, 4))
        )
    }

    @Test
    fun `using property method`() {
        val MY_GREAT_ENV_VARIABLE by EnvironmentKey.long().of().required()
        assertThat(MY_GREAT_ENV_VARIABLE(Environment.from("MY_GREAT_ENV_VARIABLE" to "123")), equalTo(123))
    }

    @Test
    fun `can get ports from env`() {
        val withPorts = env.with(EnvironmentKey.k8s.SERVICE_PORT of 80, EnvironmentKey.k8s.HEALTH_PORT of 81)
        assertThat(EnvironmentKey.k8s.SERVICE_PORT(withPorts), equalTo(80))
        assertThat(EnvironmentKey.k8s.HEALTH_PORT(withPorts), equalTo(81))
    }

    @Test
    fun `get uri for a service`() {
        assertThat(
            EnvironmentKey.k8s.serviceUriFor("myservice")(
                Environment.from("MYSERVICE_SERVICE_PORT" to "8000")
            ),
            equalTo(Uri.of("http://myservice:8000/"))
        )

        assertThat(
            EnvironmentKey.k8s.serviceUriFor("myservice")(
                Environment.from("MYSERVICE_SERVICE_PORT" to "80")
            ),
            equalTo(Uri.of("http://myservice/"))
        )

        assertThat(
            EnvironmentKey.k8s.serviceUriFor("myservice", true)(
                Environment.from("MYSERVICE_SERVICE_PORT" to "80")
            ),
            equalTo(Uri.of("https://myservice/"))
        )

        assertThat(
            EnvironmentKey.k8s.serviceUriFor("myservice")(
                Environment.from("MYSERVICE_SERVICE_PORT" to "443")
            ),
            equalTo(Uri.of("http://myservice/"))
        )

        assertThat(
            EnvironmentKey.k8s.serviceUriFor("myservice", true)(
                Environment.from("MYSERVICE_SERVICE_PORT" to "443")
            ),
            equalTo(Uri.of("https://myservice/"))
        )
    }

    @Test
    fun `falls back to value when using environment key`() {
        val finalEnv = Environment.EMPTY overrides Environment.from("FOO" to "bill")

        val key = EnvironmentKey.required("FOO")
        assertThat(finalEnv[key], equalTo("bill"))
        assertThat(key(finalEnv), equalTo("bill"))
    }

    @Test
    fun `composite can use a mixture of overridden and non overridden values`() {
        data class Target(val foo: String, val bar: Int, var foobar: Int?)

        val finalEnv = Environment.from("bar" to "123") overrides Environment.from("FOO" to "bill")

        val key = EnvironmentKey.composite {
            Target(
                required("FOO")(it),
                int().required("BAR")(it),
                int().optional("FOOBAR")(it)
            )
        }

        assertThat(key(finalEnv), equalTo(Target("bill", 123, null)))
    }

    enum class Foo {
        bar
    }

    @Test
    fun `enum support`() {
        val key = EnvironmentKey.enum<Foo>().required("foo")
        assertThat(key(Environment.EMPTY.with(key of Foo.bar)), equalTo(Foo.bar))
    }
}
