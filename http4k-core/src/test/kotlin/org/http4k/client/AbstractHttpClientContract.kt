package org.http4k.client

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.http4k.util.RetryRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.util.*

abstract class AbstractHttpClientContract(private val serverConfig: (Int) -> ServerConfig) {
    @Rule
    @JvmField
    var retryRule = RetryRule(10)

    private var server: Http4kServer? = null

    val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        val defaultHandler = { request: Request ->
            Response(Status.OK)
                .header("uri", request.uri.toString())
                .header("header", request.header("header"))
                .header("query", request.query("query"))
                .body(request.body)
        }
        server = routes("/someUri" bind Method.POST to defaultHandler,
            "/empty" bind Method.GET to { _: Request -> Response(Status.OK).body("") },
            "/redirect" bind Method.GET to { _: Request -> Response(Status.FOUND).header("Location", "/someUri").body("") },
            "/stream" bind Method.GET to { _: Request -> Response(Status.OK).body("stream".byteInputStream()) },
            "/delay/{millis}" bind Method.GET to { r: Request ->
                Thread.sleep(r.path("millis")!!.toLong())
                Response(Status.OK)
            },
            "/echo" bind Method.POST to { request: Request -> Response(Status.OK).body(request.bodyString()) },
            "/check-image" bind Method.POST to { request: Request ->
                if (Arrays.equals(testImageBytes(), request.body.payload.array()))
                    Response(Status.OK) else Response(Status.BAD_REQUEST.description("Image content does not match"))
            },
            "/status/{status}" bind Method.GET to  { r: Request ->
                val status = Status(r.path("status")!!.toInt(), "")
                Response(status).body("body for status ${status.code}")
            })
            .asServer(serverConfig(port)).start()
    }

    protected fun testImageBytes() = this::class.java.getResourceAsStream("/test.png").readBytes()

    @After
    fun after() {
        server?.stop()
    }
}