package org.http4k.k8s

import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.server.SunHttp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Http4kK8sServerTest {

    private val app: HttpHandler = { Response(I_M_A_TEAPOT) }
    private val server = app.asK8sServer(::SunHttp, 0, healthPort = 0)

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
        client(Request(GET, "http://localhost:${server.port()}")) shouldMatch hasStatus(I_M_A_TEAPOT)
    }

    @Test
    fun `health is available`() {
        client(Request(GET, "http://localhost:${server.healthPort()}/liveness")) shouldMatch hasStatus(OK)
        client(Request(GET, "http://localhost:${server.healthPort()}/readiness")) shouldMatch hasStatus(OK)
    }
}