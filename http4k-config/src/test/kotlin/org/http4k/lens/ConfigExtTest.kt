package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.config.Authority
import org.http4k.config.Host
import org.http4k.config.Port
import org.http4k.config.Secret
import org.http4k.config.Timeout
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.junit.jupiter.api.Test
import java.time.Duration

class ConfigExtTest {
    @Test
    fun port() = BiDiLensContract.checkContract(
        BiDiLensContract.spec.port(),
        Port(123),
        "123",
        "",
        "invalid",
        "o",
        "o123",
        "o123123"
    )

    @Test
    fun host() = BiDiLensContract.checkContract(
        BiDiLensContract.spec.host(),
        Host("localhost.com"),
        "localhost.com",
        "",
        null,
        "o",
        "olocalhost.com",
        "olocalhost.comlocalhost.com"
    )

    @Test
    fun authority() = BiDiLensContract.checkContract(
        BiDiLensContract.spec.authority(),
        Authority(Host.localhost, Port(80)),
        "localhost:80",
        "",
        null,
        "o",
        "olocalhost:80",
        "olocalhost:80localhost:80"
    )

    @Test
    fun timeout() = BiDiLensContract.checkContract(
        BiDiLensContract.spec.timeout(),
        Timeout(Duration.ofSeconds(35)),
        "PT35S",
        "",
        "invalid",
        "o",
        "oPT35S",
        "oPT35SPT35S"
    )

    @Test
    fun secret() {
        val requiredLens = BiDiLensContract.spec.secret().required("hello")
        assertThat(requiredLens("123"), equalTo(Secret("123".toByteArray())))
        assertThat(
            { requiredLens("") },
            throws(lensFailureWith<String>(Missing(requiredLens.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `host header`() {
        fun assertFormat(input: Authority) {
            val reqWithHeader = Request(Method.GET, "").with(Header.HOST of input)
            assertThat(reqWithHeader.header("Host"), equalTo(input.toString()))
            assertThat(org.http4k.lens.Header.HOST(reqWithHeader), equalTo(input))
        }

        assertFormat(Authority(Host.localhost, Port(443)))
        assertFormat(Authority(Host.localhost))
    }
}
