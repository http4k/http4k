package org.http4k.streaming

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.management.ManagementFactory
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


abstract class StreamingContract(private val config: StreamingTestConfiguration = StreamingTestConfiguration()) {
    private val runningInIdea = ManagementFactory.getRuntimeMXBean().inputArguments.find { it.contains("idea") } != null

    private val port = Random().nextInt(1000) + 8000
    private var runningServer: Http4kServer? = null

    private val sharedList = CopyOnWriteArrayList<Char>()

    abstract fun serverConfig(port: Int): ServerConfig
    abstract fun createClient(): HttpHandler

    private var countdown: CountDownLatch = CountDownLatch(config.beeps * 2)

    val server = routes(
        "/stream-response" bind GET to { _: Request -> Response(Status.OK).body(beeper()) },
        "/stream-request" bind POST to { request: Request ->
            captureReceivedStream { request.body.stream }; Response(Status.OK)
        }
    )

    @BeforeEach
    fun `set up`() {
        runningServer = server.asServer(serverConfig(port)).start()
        countdown = CountDownLatch(config.beeps * 2)
    }

    @AfterEach
    fun `tear down`() {
        runningServer?.stop()
    }

    @Test
    fun `can stream response`() {
        captureReceivedStream { createClient()(Request(GET, "http://localhost:$port/stream-response")).body.stream }

        waitForCompletion()

        verifyStreamed()
    }

    @Test
    fun `can stream request`() {
        createClient()(Request(POST, "http://localhost:$port/stream-request").body(beeper()))

        waitForCompletion()

        verifyStreamed()
    }

    private fun verifyStreamed() {
        assertThat(sharedList.joinToString(""), !equalTo("sssssrrrrr"))
    }

    private fun waitForCompletion() {
        val succeeded: Boolean = countdown.await(config.maxTotalWaitInMillis, TimeUnit.MILLISECONDS)
        if (!succeeded) fail("Timed out waiting for server response")
    }

    private fun captureReceivedStream(streamSource: () -> InputStream) {
        val responseStream = streamSource()

        responseStream.bufferedReader().forEachLine {
            if (runningInIdea) println("received")

            sharedList.add('r')
            countdown.countDown()
        }
    }

    private fun beeper(): InputStream {
        val input = PipedInputStream()
        val output = PipedOutputStream(input)

        val line = "b".repeat(config.beepSize) + "\n"

        thread {
            (1..5).forEach {
                if (runningInIdea) println("sent")

                output.write(line.toByteArray())
                sharedList.add('s')
                output.flush()
                countdown.countDown()
                Thread.sleep(config.sleepTimeBetweenBeepsInMillis)
            }
            output.close()
        }

        return input
    }
}

data class StreamingTestConfiguration(val beeps: Int = 5,
                                      val beepSize: Int = 20000,
                                      val sleepTimeBetweenBeepsInMillis: Long = 500) {
    val maxTotalWaitInMillis = beeps * sleepTimeBetweenBeepsInMillis * 2
}