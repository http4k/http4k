package integration

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.client.toClientStatus
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.ServerConfig.UnsupportedStopMode
import org.http4k.testing.ServerBackend
import org.http4k.testing.ServerInDocker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertTimeout
import org.opentest4j.TestAbortedException
import java.io.IOException
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

abstract class ServerStopContract(
    private val backend: ServerBackend,
    protected val client: HttpHandler,
    enableStopModes: ConfigureServerStopContract.() -> Unit
) {

    private val defaultGracefulStopMode = Graceful(ofSeconds(10))
    private val timeoutTolerance = ofMillis(1000)
    private val supportedStopModes: Set<StopMode>

    init {
        supportedStopModes = ConfigureServerStopContract()
            .also(enableStopModes)
            .enabledModes
    }

    inner class ConfigureServerStopContract(val enabledModes: MutableSet<StopMode> = mutableSetOf()) {
        fun enableImmediateStop() {
            enabledModes.add(Immediate)
        }

        fun enableGracefulStop() {
            enabledModes.add(defaultGracefulStopMode)
        }
    }

    private val Http4kServer.baseUrl: String get() = "http://localhost:${port()}"

    private fun Http4kServer.waitUntilHealthy(): Http4kServer {
        val startTime = System.currentTimeMillis()
        while (true) {
            try {
                assertThat(client(Request(GET, "${this.baseUrl}/health")), hasStatus(Status.OK))
                break;
            } catch (e: AssertionError) {
                if (System.currentTimeMillis() - startTime >= 2000) {
                    throw e
                }
            }
        }
        return this
    }

    private fun serverInDocker(stopMode: StopMode) = object : Http4kServer {
        val serverInDocker = ServerInDocker()
        val port = 8000
        override fun start(): Http4kServer {
            val containerId = serverInDocker.start(backend, stopMode)
            return object : Http4kServer {
                override fun start(): Http4kServer = error("already started")
                override fun stop(): Http4kServer {
                    serverInDocker.stop(containerId)
                    return object : Http4kServer {
                        override fun start(): Http4kServer = error("already stopped")
                        override fun stop(): Http4kServer = error("already stopped")
                        override fun port(): Int = port
                    }
                }

                override fun port(): Int = port
            }
        }

        override fun stop(): Http4kServer = error("server not started")

        override fun port() = 8000

    }

    private fun prepareServer(stopMode: StopMode) = serverInDocker(stopMode).start()

    private fun startServerOrSkip(stopMode: StopMode): Http4kServer {
        if (!supportedStopModes.contains(stopMode)) {
            throw TestAbortedException("$stopMode is not supported")
        }
        return prepareServer(stopMode).waitUntilHealthy()
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
    fun `server config throws when invoked with unsupported stop mode`() {
        val illegalConfigurationAttempts: Array<() -> Unit> =
            listOf(Immediate, defaultGracefulStopMode)
                .subtract(supportedStopModes)
                .map { stopMode ->
                    fun() {
                        assertThrows<UnsupportedStopMode>("should throw when invoked with $stopMode") {
                            backend(
                                stopMode
                            )
                        }
                    }
                }.toTypedArray()

        assertAll(*illegalConfigurationAttempts)
    }

    private fun Http4kServer.testInflightRequestsCompleteDuringServerStop() {
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
}
