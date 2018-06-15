package org.http4k.client

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
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
import java.util.Arrays
import java.util.Random

abstract class AbstractHttpClientContract(private val serverConfig: (Int) -> ServerConfig) {
    @Rule
    @JvmField
    var retryRule = RetryRule.LOCAL

    private var server: Http4kServer? = null

    val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        val defaultHandler = { request: Request ->
            Response(OK)
                .header("uri", request.uri.toString())
                .header("header", request.header("header"))
                .header("query", request.query("query"))
                .body(request.body)
        }
        val app = routes("/someUri" bind POST to defaultHandler,
                "/cookies/set" bind GET to { req: Request ->
                    Response(FOUND).header("Location", "/cookies").cookie(Cookie(req.query("name")!!, req.query("value")!!))
                },
                "/cookies" bind GET to { req: Request ->
                    Response(OK).body(req.cookies().joinToString(",") { it.name + "=" + it.value })
                },
                "/empty" bind GET to { _: Request -> Response(OK).body("") },
                "/relative-redirect/{times}" bind GET to { req: Request ->
                    val times = req.path("times")?.toInt() ?: 0
                    if (times == 0) Response(OK)
                    else Response(FOUND).header("Location", "/relative-redirect/${times - 1}")
                },
                "/redirect" bind GET to { _: Request -> Response(FOUND).header("Location", "/someUri").body("") },
                "/stream" bind GET to { _: Request -> Response(OK).body("stream".byteInputStream()) },
                "/delay/{millis}" bind GET to { r: Request ->
                    Thread.sleep(r.path("millis")!!.toLong())
                    Response(OK)
                },
                "/echo" bind routes(
                        DELETE to { _: Request -> Response(OK).body("delete") },
                        GET to { request: Request -> Response(OK).body(request.uri.toString()) },
                        POST to { request: Request -> Response(OK).body(request.bodyString()) }
                ),
                "/headers" bind { request: Request -> Response(OK).body(request.headers.joinToString(",") { it.first }) },
                "/check-image" bind POST to { request: Request ->
                    if (Arrays.equals(testImageBytes(), request.body.payload.array()))
                        Response(OK) else Response(BAD_REQUEST.description("Image content does not match"))
                },
                "/status/{status}" bind GET to { r: Request ->
                    val status = Status(r.path("status")!!.toInt(), "")
                    Response(status).body("body for status ${status.code}")
                })
        server = app
            .asServer(serverConfig(port)).start()
    }

    protected fun testImageBytes() = this::class.java.getResourceAsStream("/test.png").readBytes()

    @After
    fun after() {
        server?.stop()
    }
}