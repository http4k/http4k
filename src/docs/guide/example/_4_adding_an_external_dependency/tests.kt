package guide.example._4_adding_an_external_dependency

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import guide.example._4_adding_an_external_dependency.Matchers.answerShouldBe
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
import java.util.Random


object Matchers {

    fun Response.answerShouldBe(expected: Int) {
        this shouldMatch hasStatus(OK).and(hasBody(expected.toString()))
    }
}

abstract class RecorderCdc {
    abstract val client: HttpHandler

    @Test
    fun `records answer`() {
        Recorder(client).record(123)
        checkAnswerRecorded()
    }

    open fun checkAnswerRecorded(): Unit {}
}

class FakeRecorderHttp : HttpHandler {
    val calls = mutableListOf<Int>()

    private val answer = Path.int().of("answer")

    private val app = CatchLensFailure.then(
        routes(
            "/{answer}" bind POST to { request -> calls.add(answer.extract(request)); Response(ACCEPTED) }
        )
    )

    override fun invoke(request: Request): Response = app(request)
}

class FakeRecorderTest : RecorderCdc() {
    override val client = FakeRecorderHttp()

    override fun checkAnswerRecorded() {
        client.calls shouldMatch equalTo(listOf(123))
    }
}

@Disabled // this obviously doesn't exist, so we ignore it here
class RealRecorderTest : RecorderCdc() {
    override val client = SetHostFrom(Uri.of("http://realrecorder")).then(OkHttp())
}

class EndToEndTest {
    private val port = Random().nextInt(1000) + 8000
    private val recorderPort = port+1
    private val client = OkHttp()
    private val recorder = FakeRecorderHttp()
    private val server = MyMathServer(port, Uri.of("http://localhost:$recorderPort"))
    private val recorderServer = recorder.asServer(Jetty(recorderPort))

    @BeforeEach
    fun setup(): Unit {
        recorderServer.start()
        server.start()
    }

    @AfterEach
    fun teardown(): Unit {
        server.stop()
        recorderServer.stop()
    }

    @Test
    fun `all endpoints are mounted correctly`() {
        client(Request(GET, "http://localhost:$port/ping")) shouldMatch hasStatus(OK)
        client(Request(GET, "http://localhost:$port/add?value=1&value=2")).answerShouldBe(3)
        client(Request(GET, "http://localhost:$port/multiply?value=2&value=4")).answerShouldBe(8)
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
        env.recorder.calls shouldMatch equalTo(listOf(3))
    }

    @Test
    fun `answer is zero when no values`() {
        env.client(Request(GET, "/add")).answerShouldBe(0)
        env.recorder.calls shouldMatch equalTo(listOf(0))
    }

    @Test
    fun `bad request when some values are not numbers`() {
        env.client(Request(GET, "/add?value=1&value=notANumber")) shouldMatch hasStatus(BAD_REQUEST)
        env.recorder.calls.isEmpty() shouldMatch equalTo(true)
    }
}

class MultiplyFunctionalTest {
    private val env = AppEnvironment()

    @Test
    fun `products values together`() {
        env.client(Request(GET, "/multiply?value=2&value=4")).answerShouldBe(8)
        env.recorder.calls shouldMatch equalTo(listOf(8))
    }

    @Test
    fun `answer is zero when no values`() {
        env.client(Request(GET, "/multiply")).answerShouldBe(0)
        env.recorder.calls shouldMatch equalTo(listOf(0))
    }

    @Test
    fun `bad request when some values are not numbers`() {
        env.client(Request(GET, "/multiply?value=1&value=notANumber")) shouldMatch hasStatus(BAD_REQUEST)
        env.recorder.calls.isEmpty() shouldMatch equalTo(true)
    }
}
