package org.http4k.k8s

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.k8s.K8sEnvKey.HEALTH_PORT
import org.http4k.k8s.K8sEnvKey.SERVICE_PORT
import org.http4k.k8s.K8sEnvironment.Companion.from
import org.http4k.lens.LensFailure
import org.http4k.lens.int
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class K8sEnvKeyTest {

    private val env = K8sEnvironment.from(emptyMap())

    @Test
    fun `custom key roundtrip`() {
        val lens = K8sEnvKey.int().required("some-value")
        assertThrows<LensFailure> { lens(env) }

        val withInjectedValue = env.with(lens of 80)
        assertThat(lens(withInjectedValue), equalTo(80))
        assertThat(K8sEnvKey.int().required("SOME_VALUE")(withInjectedValue), equalTo(80))
    }

    @Test
    fun `can get ports from env`() {
        val withPorts = env.with(SERVICE_PORT of 80, HEALTH_PORT of 81)
        assertThat(SERVICE_PORT(withPorts), equalTo(80))
        assertThat(HEALTH_PORT(withPorts), equalTo(81))
    }

    @Test
    fun `get uri for a service`() {
        assertThat(K8sEnvKey.serviceUriFor("myservice")(
            from(mapOf("MYSERVICE_SERVICE_PORT" to "8000"))),
            equalTo(Uri.of("http://myservice:8000/")))

        assertThat(K8sEnvKey.serviceUriFor("myservice")(
            from(mapOf("MYSERVICE_SERVICE_PORT" to "80"))),
            equalTo(Uri.of("http://myservice/")))

        assertThat(K8sEnvKey.serviceUriFor("myservice", true)(
            from(mapOf("MYSERVICE_SERVICE_PORT" to "80"))),
            equalTo(Uri.of("https://myservice/")))

        assertThat(K8sEnvKey.serviceUriFor("myservice")(
            from(mapOf("MYSERVICE_SERVICE_PORT" to "443"))),
            equalTo(Uri.of("http://myservice/")))

        assertThat(K8sEnvKey.serviceUriFor("myservice", true)(
            from(mapOf("MYSERVICE_SERVICE_PORT" to "443"))),
            equalTo(Uri.of("https://myservice/")))
    }
}