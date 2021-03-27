package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.function.Supplier
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.nio.AsyncPushConsumer
import org.apache.hc.core5.http.nio.AsyncRequestProducer
import org.apache.hc.core5.http.nio.AsyncResponseConsumer
import org.apache.hc.core5.http.nio.HandlerFactory
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.reactor.IOReactorStatus
import org.apache.hc.core5.util.TimeValue
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.hamkrest.hasStatus
import org.http4k.server.SunHttp
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class ApacheAsyncClientTest : AsyncHttpClientContract(::SunHttp, ApacheAsyncClient(),
    ApacheAsyncClient(HttpAsyncClients.custom()
        .setIOReactorConfig(IOReactorConfig.custom()
            .setSoTimeout(100, TimeUnit.MILLISECONDS)
            .build()).build())) {
    @Test
    fun `connect timeout is handled`() {

        val latch = CountDownLatch(1)
        ApacheAsyncClient(object : CloseableHttpAsyncClient() {
            override fun start() {}

            override fun close(closeMode: CloseMode?) {}

            override fun getStatus(): IOReactorStatus = IOReactorStatus.ACTIVE

            override fun awaitShutdown(waitTime: TimeValue?) {}

            override fun register(hostname: String?, uriPattern: String?, supplier: Supplier<AsyncPushConsumer>?) {}

            override fun <T : Any?> doExecute(target: HttpHost?, requestProducer: AsyncRequestProducer?, responseConsumer: AsyncResponseConsumer<T>?, pushHandlerFactory: HandlerFactory<AsyncPushConsumer>?, context: HttpContext?, callback: FutureCallback<T>?): Future<T> {
                callback?.failed(ConnectTimeoutException("test timeout"))
                return CompletableFuture.completedFuture(null)
            }

            override fun initiateShutdown() {}

            override fun close() {}
        })(Request(GET, "http://localhost:8000")) {
            assertThat(it, hasStatus(CLIENT_TIMEOUT))
            latch.countDown()
        }

        latch.await()
    }
}
