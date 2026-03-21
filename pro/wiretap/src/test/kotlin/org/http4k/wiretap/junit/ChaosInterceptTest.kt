package org.http4k.wiretap.junit

import App
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosEngine
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChaosInterceptTest {

    private val downstream: HttpHandler = { Response(OK).body("downstream") }

    @RegisterExtension
    @JvmField
    val intercept = Intercept(downstream, Always) {
        App(http(), "test app 1", otel("test app 1"))
    }

    @Test
    fun `outbound calls succeed when chaos is disabled`(http: HttpHandler) {
        val response = http(Request(GET, "/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `outbound calls fail when chaos is enabled`(http: HttpHandler, chaos: ChaosEngine) {
        chaos.enable(ReturnStatus(INTERNAL_SERVER_ERROR))

        val response = http(Request(GET, "/"))
        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }
}
