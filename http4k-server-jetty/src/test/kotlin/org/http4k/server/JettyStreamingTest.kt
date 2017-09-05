package org.http4k.server

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import junit.framework.Assert.fail
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.http4k.client.ApacheClient
import org.http4k.client.ResponseBodyMode
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.Ignore
import org.junit.Test


class JettyStreamingTest {
    @Test
    @Ignore("fails on CI")
    fun `release streaming connections`() {
        val threadPool = QueuedThreadPool(10)
        val server = Server(threadPool)
        val connector = ServerConnector(server)
        server.addConnector(connector)

        val sizableResponse = (1..5000000).joinToString("x")

        val app = { _: Request -> Response(Status.OK).body(sizableResponse.byteInputStream()) }
        app.asServer(Jetty(server)).start()

        val initialUsedThreads = threadPool.busyThreads

        val port = server.uri.port

        val client = ApacheClient(bodyMode = ResponseBodyMode.Stream)

        val response = client(Request(Method.GET, "http://localhost:$port/"))

        threadPool.busyThreads.shouldMatch(equalTo(initialUsedThreads + 1))

        response.body.stream.close()

        tryFor(10, "request thread was not released") {
            threadPool.busyThreads.shouldMatch(equalTo(initialUsedThreads))
        }
    }

    private fun tryFor(timeoutInSeconds: Int, description: String, assertion: () -> Unit) {
        val start = System.currentTimeMillis()
        do {
            try {
                assertion()
                return
            } catch (e: AssertionError) {
                Thread.sleep(100)
            }
        } while (System.currentTimeMillis() - start < timeoutInSeconds * 1000)
        fail("Test timed out: $description")
    }
}

