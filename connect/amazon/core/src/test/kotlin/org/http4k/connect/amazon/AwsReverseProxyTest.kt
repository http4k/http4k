package org.http4k.connect.amazon

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class AwsReverseProxyTest {

    object Service1 : AwsServiceCompanion("service1")
    object Service2 : AwsServiceCompanion("service2")

    @Test
    fun `routes to correct AWS handler based on host header`() {
        val reverseProxy = AwsReverseProxy(
            Service1 to { Response(OK) },
            Service2 to { Response(I_M_A_TEAPOT) }
        )

        assertThat(reverseProxy(Request(GET, "").header("host", "foo.service1.aws")), hasStatus(OK))
        assertThat(reverseProxy(Request(GET, "").header("host", "foo.service2.aws")), hasStatus(I_M_A_TEAPOT))
        assertThat(reverseProxy(Request(GET, "").header("host", "foo.service3.aws")), hasStatus(NOT_FOUND))
    }

}
