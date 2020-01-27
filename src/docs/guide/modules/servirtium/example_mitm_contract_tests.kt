package guide.modules.servirtium.mitm

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.servirtium.InteractionOptions
import org.http4k.servirtium.ServirtiumServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

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

    override val uri get() = Uri.of("http://localhost:${servirtium.port()}")

    private val app = WordCounterApp(0)
    private lateinit var servirtium: Http4kServer

    @BeforeEach
    fun start(info: TestInfo) {
        val appPort = app.start().port()
        servirtium = ServirtiumServer.Recording(
            info.displayName.removeSuffix("()"),
            Uri.of("http://localhost:$appPort"),
            options = object : InteractionOptions {
                override fun modify(request: Request) = request.removeHeader("Host").removeHeader("User-agent")
                override fun modify(response: Response) = response.removeHeader("Date")
            }
        ).start()
    }

    @AfterEach
    fun stop() {
        app.stop()
        servirtium.stop()
    }
}

/**
 * Replays incoming traffic from disk. MiTM starts on a random port.
 */
@Disabled
class MiTMReplayingWordCounterTest : WordCounterContract {

    override val uri get() = Uri.of("http://localhost:${servirtium.port()}")

    private lateinit var servirtium: Http4kServer

    @BeforeEach
    fun start(info: TestInfo) {
        servirtium = ServirtiumServer.Replay(info.displayName.removeSuffix("()"), options = object : InteractionOptions {
            override fun modify(request: Request) = request.header("Date", "some overridden date")
        }).start()
    }

    @AfterEach
    fun stop() {
        servirtium.stop()
    }
}
