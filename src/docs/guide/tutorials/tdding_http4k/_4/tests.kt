package guide.tutorials.tdding_http4k._4

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import guide.tutorials.tdding_http4k._4.Matchers.answerShouldBe
import java.util.Random

object Matchers {
    fun Response.answerShouldBe(expected: Int) {
        assertThat(this, hasStatus(OK).and(hasBody(expected.toString())))
    }
}

abstract class RecorderCdc {
    abstract val client: HttpHandler

    @Test
    fun `records answer`() {
        Recorder(client).record(123)
        checkAnswerRecorded()
    }

    open fun checkAnswerRecorded() {}
}

class FakeRecorderHttp : HttpHandler {
    val calls = mutableListOf<Int>()

    private val answer = Path.int().of("answer")

    private val app = CatchLensFailure.then(
        routes(
            "/{answer}" bind POST to { request -> calls.add(answer(request)); Response(ACCEPTED) }
        )
    )

    override fun invoke(request: Request): Response = app(request)
}

class FakeRecorderTest : RecorderCdc() {
    override val client = FakeRecorderHttp()

    override fun checkAnswerRecorded() {
        assertThat(client.calls, equalTo(listOf(123)))
    }
}

@Disabled // this obviously doesn't exist, so we ignore it here
class RealRecorderTest : RecorderCdc() {
    override val client = SetHostFrom(Uri.of("http://realrecorder")).then(OkHttp())
}

class EndToEndTest {
    private val port = Random().nextInt(1000) + 8000
    private val recorderPort = port + 1
    private val client = OkHttp()
    private val recorder = FakeRecorderHttp()
    private val server = MyMathServer(0, Uri.of("http://localhost:$recorderPort"))
    private val recorderServer = recorder.asServer(Jetty(recorderPort))

    @BeforeEach
    fun setup() {
        recorderServer.start()
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.stop()
        recorderServer.stop()
    }

    @Test
    fun `all endpoints are mounted correctly`() {
        assertThat(
            client(Request(GET, "http://localhost:${server.port()}/ping")),
            hasStatus(OK)
        )
        client(
            Request(
                GET,
                "http://localhost:${server.port()}/add?value=1&value=2"
            )
        ).answerShouldBe(3)
        client(
            Request(
                GET,
                "http://localhost:${server.port()}/multiply?value=2&value=4"
            )
        ).answerShouldBe(8)
    }
}

class AppEnvironment {
    val recorder = FakeRecorderHttp()
    val client = MyMathsApp(recorder)
}

class AddFunctionalTest {
    private val env = AppEnvironment()

    @Test
    fun `adds values together`() {
        env.client(Request(GET, "/add?value=1&value=2")).answerShouldBe(3)
        assertThat(env.recorder.calls, equalTo(listOf(3)))
    }

    @Test
    fun `answer is zero when no values`() {
        env.client(Request(GET, "/add")).answerShouldBe(0)
        assertThat(env.recorder.calls, equalTo(listOf(0)))
    }

    @Test
    fun `bad request when some values are not numbers`() {
        assertThat(
            env.client(Request(GET, "/add?value=1&value=notANumber")),
            hasStatus(BAD_REQUEST)
        )
        assertThat(env.recorder.calls.isEmpty(), equalTo(true))
    }
}

class MultiplyFunctionalTest {
    private val env = AppEnvironment()

    @Test
    fun `products values together`() {
        env.client(Request(GET, "/multiply?value=2&value=4")).answerShouldBe(8)
        assertThat(env.recorder.calls, equalTo(listOf(8)))
    }

    @Test
    fun `answer is zero when no values`() {
        env.client(Request(GET, "/multiply")).answerShouldBe(0)
        assertThat(env.recorder.calls, equalTo(listOf(0)))
    }

    @Test
    fun `bad request when some values are not numbers`() {
        assertThat(
            env.client(Request(GET, "/multiply?value=1&value=notANumber")),
            hasStatus(BAD_REQUEST)
        )
        assertThat(env.recorder.calls.isEmpty(), equalTo(true))
    }
}
