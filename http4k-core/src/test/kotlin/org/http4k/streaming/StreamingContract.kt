package org.http4k.streaming

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class StreamingContract(private val config: StreamingTestConfiguration = StreamingTestConfiguration()) {
    private val runningInIdea = ManagementFactory.getRuntimeMXBean().inputArguments.find { it.contains("idea") } != null

    private val port = Random().nextInt(1000) + 8000
    private var runningServer: Http4kServer? = null

    abstract fun serverConfig(port: Int): ServerConfig
    abstract fun createClient(): HttpHandler

    val server = { _: Request -> Response(Status.OK).body(beeper()) }

    @Before
    fun `set up`() {
        runningServer = server.asServer(serverConfig(port)).start()
    }

    @After
    fun `tear down`() {
        runningServer?.stop()
    }

    @Test
    fun `can stream response`() {
        val countdown = CountDownLatch(config.beeps)

        Thread {
            var lastReceived = System.currentTimeMillis()
            val responseStream = createClient()(Request(Method.GET, "http://localhost:$port")).body.stream
            var currentReceived: Long

            responseStream.bufferedReader().forEachLine { line ->
                if (runningInIdea) println("received")

                currentReceived = System.currentTimeMillis()
                if ((currentReceived - lastReceived) > config.maxWaitBetweenBeepsInMillis) {
                    Assert.fail("Timed out waiting for next line")
                }
                lastReceived = currentReceived
                countdown.countDown()
            }
        }.start()

        val succeeded: Boolean = countdown.await(config.maxTotalWaitInMillis, TimeUnit.MILLISECONDS)
        if (!succeeded)
            Assert.fail("Timed out waiting for server response")
    }

    private fun beeper(): InputStream {
        val input = PipedInputStream()
        val output = PipedOutputStream(input)

        val line = "b".repeat(config.beepSize) + "\n"

        Thread {
            (1..5).forEach {
                if (runningInIdea) println("sent")

                output.write(line.toByteArray())
                output.flush()
                Thread.sleep(config.sleepTimeBetweenBeepsInMillis)
            }
            output.close()
        }.start()

        return input
    }
}

data class StreamingTestConfiguration(val beeps: Int = 5,
                                      val beepSize: Int = 20000,
                                      val sleepTimeBetweenBeepsInMillis: Long = 500) {
    val maxWaitBetweenBeepsInMillis = sleepTimeBetweenBeepsInMillis * 3
    val maxTotalWaitInMillis = beeps * sleepTimeBetweenBeepsInMillis * 2
}