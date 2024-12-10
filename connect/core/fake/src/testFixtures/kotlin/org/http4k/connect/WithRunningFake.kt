package org.http4k.connect

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.server.Http4kServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class WithRunningFake(private val fn: () -> ChaoticHttpHandler) : PortBasedTest {

    private lateinit var server: Http4kServer

    val http: HttpHandler = {
        SetHostFrom(Uri.of("http://localhost:${server.port()}")).then(JavaHttpClient())(it)
    }

    @BeforeEach
    fun setUp() {
        server = fn().start(0)
    }

    @AfterEach
    fun stop() {
        server.stop()
    }
}
