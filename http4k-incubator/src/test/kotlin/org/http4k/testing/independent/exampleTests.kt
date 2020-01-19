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
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.testing.replayingMatchingContent
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Servirtium
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
    private val http = SetBaseUriFrom(baseUri).then(ApacheClient())
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

@Disabled
class DirectHttpRecordingWordCounterTest : WordCounterContract {
    override val uri = Uri.of("http://serverundertest:8080")
}

@Disabled
class MiTMRecordingWordCounterTest : WordCounterContract {

    override val uri get() = Uri.of("http://localhost:${mitm.port()}")

    private val app = WordCounterApp(8080)
    private lateinit var mitm: Http4kServer

    @BeforeEach
    fun start(info: TestInfo) {
        app.start()
        mitm = MiTMRecorder(info.displayName.removeSuffix("()"), Uri.of("http://localhost:8080")).start()
    }

    @AfterEach
    fun stop() {
        app.stop()
        mitm.stop()
    }
}

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

fun MiTMRecorder(name: String, target: Uri, root: File = File(".")) =
    RecordTo(ReadWriteStream.Servirtium(root, name, clean = true))
        .then(SetBaseUriFrom(target))
        .then(ApacheClient())
        .asServer(SunHttp(0))

fun MiTMReplayer(name: String, root: File = File(".")) =
    DebuggingFilters.PrintRequestAndResponse()
        .then(CatchUnmatchedRequest())
        .then(ReadWriteStream.Servirtium(root, name).replayingMatchingContent())
        .asServer(SunHttp(0))

// At the moment, the replayingMatchingContent() above throws an AssertionFailedError
fun CatchUnmatchedRequest() = Filter { next ->
    {
        try {
            next(it)
        } catch (e: AssertionFailedError) {
            e.printStackTrace()
            Response(INTERNAL_SERVER_ERROR)
        }
    }
}
