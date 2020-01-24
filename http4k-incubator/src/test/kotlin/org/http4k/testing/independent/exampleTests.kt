package org.http4k.testing.independent

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Filter
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.testing.replayingMatchingContent
import org.http4k.traffic.ByteStorage.Companion.Disk
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * This client wraps the calls to a remote WordCounter service
 */
class WordCounterClient(baseUri: Uri) {
    private val http = SetBaseUriFrom(baseUri)
        .then(ClientFilters.HandleRemoteRequestFailed())
        .then(ApacheClient())

    fun wordCount(name: String): Int = http(Request(POST, "/count").body(name)).bodyString().toInt()
}

/**
 * This is our producing app
 */
fun WordCounterApp(port: Int): Http4kServer {
    val app = routes("/count" bind POST to { req: Request ->
        Response(OK).body(
            req.bodyString().run { if (isBlank()) 0 else split(" ").size }.toString()
        )
    })
    return app.asServer(SunHttp(port))
}

/**
 * Defines the test contract which will be recorded and replayed later.
 */
interface WordCounterContract {

    val uri: Uri

    @Test
    @JvmDefault
    fun `count the number of words`() {
        assertThat(WordCounterClient(uri).wordCount("A random string with 6 words"), equalTo(6))
    }

    @Test
    @JvmDefault
    fun `empty string has zero words`() {
        assertThat(WordCounterClient(uri).wordCount(""), equalTo(0))
    }
}

/**
 * This calls the server directly
 */
@Disabled
class DirectHttpRecordingWordCounterTest : WordCounterContract {
    override val uri = Uri.of("http://serverundertest:8080")
}

/**
 * Proxies traffic to the real service and records it to disk. Both MiTM and Producer start on a random port.
 */
@Disabled
class MiTMRecordingWordCounterTest : WordCounterContract {

    override val uri get() = Uri.of("http://localhost:${mitm.port()}")

    private val app = WordCounterApp(0)
    private lateinit var mitm: Http4kServer

    @BeforeEach
    fun start(info: TestInfo) {
        val appPort = app.start().port()
        mitm = MiTMRecorder(
            info.displayName.removeSuffix("()"),
            Uri.of("http://localhost:$appPort"),
            responseManipulations = { it.removeHeader("Host").removeHeader("User-agent") }
        ).start()
    }

    @AfterEach
    fun stop() {
        app.stop()
        mitm.stop()
    }
}

/**
 * Replays incoming traffic from disk. MiTM starts on a random port.
 */
@Disabled
class MiTMReplayingWordCounterTest : WordCounterContract {

    override val uri get() = Uri.of("http://localhost:${mitm.port()}")

    private lateinit var mitm: Http4kServer

    @BeforeEach
    fun start(info: TestInfo) {
        mitm = MiTMReplayer(info.displayName.removeSuffix("()")).start()
    }

    @AfterEach
    fun stop() {
        mitm.stop()
    }
}

/**
 * MiTM recorder to store the incoming traffic. At the moment you need to pass the Uri in for the
 * target server, but if we were happy to use java system proxy settings then it would work without
 * There is no request cleaning going on here.
 */
fun MiTMRecorder(name: String, target: Uri, root: File = File("."),
                 requestManipulations: (Request) -> Request = { it },
                 responseManipulations: (Response) -> Response = { it }
) =
    RecordTo(Sink.Servirtium(Disk(File(root, "$name.md"), true), requestManipulations, responseManipulations))
        .then(SetBaseUriFrom(target))
        .then(ApacheClient())
        .asServer(SunHttp(0))

/**
 * MiTM replayer. At the moment, traffic is only checked using the headers which exist in the recording -
 * excess headers from the actual requests are discarded.
 */
fun MiTMReplayer(name: String, root: File = File("."), manipulations: (Response) -> Response = { it }) =
    CatchUnmatchedRequest()
        .then(Replay.Servirtium(Disk(File(root, "$name.md")), manipulations).replayingMatchingContent())
        .asServer(SunHttp(0))

/**
 * At the moment, the replayingMatchingContent() above throws an AssertionFailedError, so we need a filter
 * to convert the error
 */
fun CatchUnmatchedRequest() = Filter { next ->
    {
        try {
            next(it)
        } catch (e: AssertionFailedError) {
            e.printStackTrace()
            Response(INTERNAL_SERVER_ERROR).body(e.localizedMessage)
        }
    }
}
