package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.apache.http.concurrent.FutureCallback
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.apache.http.nio.protocol.HttpAsyncRequestProducer
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer
import org.apache.http.protocol.HttpContext
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.hamkrest.hasStatus
import org.http4k.server.SunHttp
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future

class ApacheAsyncClientTest : AsyncHttpClientContract({ SunHttp(it) }, ApacheAsyncClient(),
    ApacheAsyncClient(HttpAsyncClients.custom()
        .setDefaultIOReactorConfig(IOReactorConfig.custom()
            .setSoTimeout(100)
            .build()).build().apply { start() })) {
    @Test
    fun `connect timeout is handled`() {

        val latch = CountDownLatch(1)
        ApacheAsyncClient(object : CloseableHttpAsyncClient() {
            override fun isRunning(): Boolean = false
            override fun start() {}

            override fun <T : Any?> execute(rp: HttpAsyncRequestProducer?, rc: HttpAsyncResponseConsumer<T>?, context: HttpContext?, cb: FutureCallback<T>): Future<T> {
                cb.failed(ConnectTimeoutException())
                return CompletableFuture.completedFuture(null)
            }

            override fun close() {}

        })(Request(GET, "http://localhost:8000")) {
            assertThat(it, hasStatus(CLIENT_TIMEOUT))
            latch.countDown()
        }

        latch.await()
    }
}

