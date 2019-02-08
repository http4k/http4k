package org.http4k.cloudnative

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey.k8s.HEALTH_PORT
import org.http4k.cloudnative.env.EnvironmentKey.k8s.SERVICE_PORT
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.hamkrest.hasStatus
import org.http4k.server.SunHttp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Http4kK8sServerTest {

    private val app = HttpHandler { Response(I_M_A_TEAPOT) }
    private val env = Environment.EMPTY.with(SERVICE_PORT of 0, HEALTH_PORT of 0)
    private val server = app.asK8sServer(::SunHttp, env)

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop()
    }

    private val client = JavaHttpClient()

    @Test
    fun `app is available on port`() {
        assertThat(client(Request(GET, "http://localhost:${server.port()}")), hasStatus(I_M_A_TEAPOT))
    }

    @Test
    fun `health is available`() {
        assertThat(client(Request(GET, "http://localhost:${server.healthPort()}/liveness")), hasStatus(OK))
        assertThat(client(Request(GET, "http://localhost:${server.healthPort()}/readiness")), hasStatus(OK))
    }
}