package org.http4k.server

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isWithin
import org.http4k.client.toClientStatus
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.binary
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Delayed
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.ServerConfig.UnsupportedStopMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertTimeout
import org.opentest4j.TestAbortedException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

abstract class ServerStopContract(
    private val serverConfig: (StopMode) -> ServerConfig,
    protected val client: HttpHandler,
    enableStopModes: ConfigureServerStopContract.() -> Unit
) {

    private val defaultGracefulStopMode = Graceful(ofSeconds(3))
    private val defaultDelayedStopMode = Delayed(ofSeconds(3))
    private val timeoutTolerance = ofMillis(200)

    private val supportedStopModes: Set<StopMode>

    init {
        supportedStopModes = ConfigureServerStopContract()
            .also(enableStopModes)
            .enabledModes
    }

    inner class ConfigureServerStopContract(val enabledModes: MutableSet<StopMode> = mutableSetOf()) {
        fun enableImmediateStop() { enabledModes.add(Immediate) }
        fun enableGracefulStop() { enabledModes.add(defaultGracefulStopMode) }
        fun enableDelayedStop() { enabledModes.add(defaultDelayedStopMode) }
    }

    private val Http4kServer.baseUrl: String get() = "http://localhost:${port()}"

    private fun Http4kServer.waitUntilHealthy(): Http4kServer {
        val startTime = System.currentTimeMillis()
        while (true) {
            try {
                assertThat(client(Request(GET, "${this.baseUrl}/health")), hasStatus(Status.OK))
                break;
            } catch (e: AssertionError) {
                if (System.currentTimeMillis() - startTime >= 2000) { throw e }
            }
        }
        return this
    }

    private val routes =
        listOf(
            "/health" bind GET to { Response(Status.OK).body("UP") },
            "/slow-echo" bind POST to slowEchoHandler(ofMillis(500))
        )

    private fun prepareServer(stopMode: StopMode) = routes(*routes.toTypedArray()).asServer(serverConfig(stopMode))
    private fun startServerOrSkip(stopMode: StopMode): Http4kServer {
        if (!supportedStopModes.contains(stopMode)) {
            throw TestAbortedException("$stopMode is not supported")
        }
        return prepareServer(stopMode).start().waitUntilHealthy()
    }

    private fun Http4kServer.testBlockingStop() {
        val request = Request(GET, "${baseUrl}/health")
        stop()

        val response = client(request)

        assertThat(response, hasStatus(Status.CONNECTION_REFUSED))
    }

    @Test
    fun `immediate stop mode is blocking on stop`() {
        startServerOrSkip(Immediate).testBlockingStop()
    }

    @Test
    fun `graceful stop mode is blocking on stop`() {
        startServerOrSkip(defaultGracefulStopMode).testBlockingStop()
    }

    @Test
    fun `delayed stop mode is blocking on stop`() {
        startServerOrSkip(defaultDelayedStopMode).testBlockingStop()
    }


    @Test
    fun `immediate stop mode is quick`() {
        val server = startServerOrSkip(Immediate)

        assertTimeout(timeoutTolerance) {
            server.stop()
        }
    }

    @Test
    fun `graceful stop mode takes at most the specified timeout to stop`() {
        val modeInTest = defaultGracefulStopMode
        val server = startServerOrSkip(modeInTest)

        assertTimeout(modeInTest.timeout + timeoutTolerance) {
            server.stop()
        }
    }

    @Test
    fun `delayed stop mode takes exactly the specified timeout to stop`() {
        val modeInTest = defaultDelayedStopMode
        val server = startServerOrSkip(modeInTest)

        val stopTime = assertTimeout(modeInTest.timeout + timeoutTolerance) {
            val start = Instant.now()
            server.stop()
            between(start, Instant.now())
        }
        assertThat(
                "Delayed stop expected but was Graceful stop detected " +
                        "(stop took $stopTime instead of ${modeInTest.timeout})",
                stopTime,
                isWithin((modeInTest.timeout - timeoutTolerance)..(modeInTest.timeout + timeoutTolerance)))
    }

    fun Http4kServer.testInflightRequestsCompleteDuringServerStop() {
        val responses = ConcurrentLinkedQueue<Response>()
        val numberOfInflightRequests = 5
        val countDownInflightRequestsStarted = CountDownLatch(numberOfInflightRequests)
        val inflightRequestThreads = (1..numberOfInflightRequests).map {
            thread {
                countDownInflightRequestsStarted.countDown()
                try {
                    responses.add(client(Request(POST, "$baseUrl/slow-echo").body("Hello")).also { it.bodyString() })
                } catch (e: IOException) {
                    responses.add(Response(Status.CONNECTION_REFUSED.toClientStatus(e)))
                }
            }
        }
        countDownInflightRequestsStarted.await(1, SECONDS)
        Thread.sleep(100)

        assertThat(responses.size, equalTo(0))
        stop()
        inflightRequestThreads.forEach { it.join() }

        assertThat(responses, hasSize(equalTo(numberOfInflightRequests)))
        assertThat(responses, allElements(hasStatus(Status.OK).and(hasBody("Hello"))))
    }

    @Test
    fun `immediate stop mode is cancelling inflight requests`() {
        val server = startServerOrSkip(Immediate)

        val assertion = assertThrows<AssertionError> {
            server.testInflightRequestsCompleteDuringServerStop()
        }

        assertThat(assertion.message!!, containsSubstring("Connection Refused"))
    }

    @Test
    fun `graceful stop mode is waiting for inflight requests to succeed`() {
        startServerOrSkip(defaultGracefulStopMode).testInflightRequestsCompleteDuringServerStop()
    }

    @Test
    fun `delayed stop mode is waiting for inflight requests to succeed`() {
        startServerOrSkip(defaultDelayedStopMode).testInflightRequestsCompleteDuringServerStop()
    }

    @Test
    fun `server config throws when invoked with unsupported stop mode`() {
        val illegalConfigurationAttempts: Array<() -> Unit> =
            listOf(Immediate, defaultDelayedStopMode, defaultGracefulStopMode)
                .subtract(supportedStopModes)
                .map { stopMode ->
                    fun() {
                        assertThrows<UnsupportedStopMode>("should throw when invoked with $stopMode") { prepareServer(stopMode) }
                    }
                }.toTypedArray()

        assertAll(*illegalConfigurationAttempts)
    }

    private fun slowEchoHandler(delay: Duration): (Request) -> Response = {
        busyWait(delay)
        Response(Status.OK).with(Body.binary(ContentType.TEXT_PLAIN).toLens() of SlowBodyProducer(delay, it.bodyString()))
    }

    private inner class SlowBodyProducer(val delay: Duration, message: String) : ByteArrayInputStream(message.toByteArray()) {
        var firstRead = true
        fun <T> wait(then: () -> T): T = then().also {
            if (firstRead) {
                busyWait(delay)
                firstRead = false
            }
        }
        override fun read(): Int = wait { super.read() }
        override fun read(b: ByteArray): Int = wait { super.read(b) }
        override fun read(b: ByteArray, off: Int, len: Int): Int = wait { super.read(b, off, len) }
    }

    private fun busyWait(delay: Duration) {
        val continueAt = System.currentTimeMillis() + delay.toMillis()
        @Suppress("ControlFlowWithEmptyBody")
        while (System.currentTimeMillis() < continueAt) {
        }
    }
}
