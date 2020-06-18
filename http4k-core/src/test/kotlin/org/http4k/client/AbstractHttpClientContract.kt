package org.http4k.client

import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class AbstractHttpClientContract(private val serverConfig: (Int) -> ServerConfig) {

    private lateinit var server: Http4kServer

    val port: Int
        get() = server.port()

    @BeforeEach
    fun before() {
        server = ServerForClientContract.asServer(serverConfig(0)).start()
    }

    protected fun testImageBytes() = this::class.java.getResourceAsStream("/test.png").readBytes()

    @AfterEach
    fun after() {
        server.stop()
    }
}