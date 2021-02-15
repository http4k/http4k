package cookbook.service_virtualisation.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.junit.ServirtiumRecording
import org.http4k.junit.ServirtiumReplay
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.servirtium.GitHub
import org.http4k.servirtium.InteractionStorage.Companion.Disk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.nio.file.Paths

/**
 * This client wraps the calls to a remote WordCounter service
 */
class WordCounterClient(private val http: HttpHandler) {
    fun wordCount(name: String): Int = http(Request(POST, "/count").body(name)).bodyString().toInt()
}

/**
 * This is our producing app
 */
class WordCounterApp : HttpHandler {
    override fun invoke(req: Request) = Response(OK).body(
        req.bodyString().run { if (isBlank()) 0 else split(" ").size }.toString()
    )
}

/**
 * Defines the test contract which will be recorded and replayed later. The injected HttpHandler
 * is provided by the implementations of this interface.
 */
interface WordCounterContract {

    @Test
    @JvmDefault
    fun `count the number of words`(handler: HttpHandler) {
        assertThat(WordCounterClient(handler).wordCount("A random string with 6 words"), equalTo(6))
    }

    @Test
    @JvmDefault
    fun `empty string has zero words`(handler: HttpHandler) {
        assertThat(WordCounterClient(handler).wordCount(""), equalTo(0))
    }
}

/**
 * For the traditional use-case of a CDC, we use a real Http client to record the traffic against
 * a running version of the producing service.
 */
@Disabled
class RemoteHttpRecordingWordCounterTest : WordCounterContract {

    private val app = SetHostFrom(Uri.of("http://serverundertest:8080"))
        .then(ApacheClient())

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording("WordCounter", app, Disk(File(".")))
}

/**
 * In cases where the producing service codebase:
 * 1. Has access to the wrapping Client and the ClientContract code (eg. monorepo with several services)
 * 2. Is also written in http4k
 * ... we can have the Producer implement the contract entirely in-memory without a MiTM.
 */
@Disabled
class InMemoryRecordingWordCounterTest : WordCounterContract {

    private val app = WordCounterApp()

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording("WordCounter", app, Disk(File(".")))

    @AfterEach
    fun after(handler: HttpHandler) {
        val name = "this traffic is not recorded"
        println(name + ": " + WordCounterClient(handler).wordCount(name))
    }
}

/**
 * In cases where the producing service codebase:
 * 1. Has access to the wrapping Client and the ClientContract code (eg. monorepo with several services)
 * 2. Is *not* written in http4k
 * ... we can have the Producer implement the contract by starting up the server and with a MiTM.
 */
@TestInstance(PER_CLASS)
@Disabled
class PortBoundRecordingWordCounterTest : WordCounterContract {

    @BeforeAll
    fun start() {
        // pretend that this is not an http4k service.. :)
        WordCounterApp().asServer(SunHttp(8080)).start()
    }

    private val app = SetHostFrom(Uri.of("http://localhost:8080"))
        .then(ApacheClient())

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording("WordCounter", app, Disk(File(".")))
}

@Disabled
class ReplayFromDiskTest : WordCounterContract {
    @JvmField
    @RegisterExtension
    val replay = ServirtiumReplay("WordCounter", Disk(File(".")))
}

@Disabled
class ReplayFromGitHubTest : WordCounterContract {
    @JvmField
    @RegisterExtension
    val replay = ServirtiumReplay("WordCounter",
        GitHub("http4k", "http4k",
            Credentials("<github user>", "<personal access token>"),
            Paths.get("src/test/resources/cookbook/service_virtualisation")
        )
    )
}
