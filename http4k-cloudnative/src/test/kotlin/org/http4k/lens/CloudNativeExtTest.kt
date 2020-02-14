package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.cloudnative.env.Authority
import org.http4k.cloudnative.env.Host
import org.http4k.cloudnative.env.Port
import org.http4k.cloudnative.env.Secret
import org.http4k.cloudnative.env.Timeout
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.BiDiLensContract.checkContract
import org.http4k.lens.BiDiLensContract.spec
import org.junit.jupiter.api.Test
import java.time.Duration

class CloudNativeExtTest {
    @Test
    fun port() = checkContract(spec.port(), Port(123), "123", "", "invalid", "o", "o123", "o123123")

    @Test
    fun host() = checkContract(spec.host(), Host("localhost.com"), "localhost.com", "", null, "o", "olocalhost.com", "olocalhost.comlocalhost.com")

    @Test
    fun authority() = checkContract(spec.authority(), Authority(Host.localhost, Port(80)), "localhost:80", "", null, "o", "olocalhost:80", "olocalhost:80localhost:80")

    @Test
    fun timeout() = checkContract(spec.timeout(), Timeout(Duration.ofSeconds(35)), "PT35S", "", "invalid", "o", "oPT35S", "oPT35SPT35S")

    @Test
    fun secret() {
        val requiredLens = spec.secret().required("hello")
        assertThat(requiredLens("123"), equalTo(Secret("123".toByteArray())))
        assertThat({ requiredLens("") }, throws(lensFailureWith<String>(Missing(requiredLens.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `host header`() {
        fun assertFormat(input: Authority) {
            val reqWithHeader = Request(GET, "").with(Header.HOST of input)
            assertThat(reqWithHeader.header("Host"), equalTo(input.toString()))
            assertThat(Header.HOST(reqWithHeader), equalTo(input))
        }

        assertFormat(Authority(Host.localhost, Port(443)))
        assertFormat(Authority(Host.localhost))
    }

}
