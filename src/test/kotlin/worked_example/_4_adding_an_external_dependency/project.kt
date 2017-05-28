package worked_example._4_adding_an_external_dependency

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.by
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import worked_example._4_adding_an_external_dependency.Matchers.answerShouldBe
import worked_example._4_adding_an_external_dependency.Matchers.statusShouldBe

/**
 * 4. Adding an external dependency.
 * At this point, the separation of the layers starts to become clear:
 * - The server layer is responsible for taking external configuration and instantiating the app layer.
 * - The application layer API is only in terms of HTTP transports - it constructs business level abstractions
 * which are passed down into to the individual endpoints
 *
 * The process here is to create fake versions of the dependency which can be tested against through the business interface.
 * This requires another style of testing, CDCs (Consumer Driven Contracts), to be created. These contract tests ensure that our
 * interactions with the external service are valid.
 *
 * REQUIREMENTS:
 * - Results from calculations should be POSTed via HTTP to another "answer recording" service.
 */

/** TESTS **/
object Matchers {
    fun Response.statusShouldBe(expected: Status) = status shouldMatch equalTo(expected)

    fun Response.answerShouldBe(expected: Int) {
        statusShouldBe(OK)
        bodyString().toInt() shouldMatch equalTo(expected)
    }
}

class FakeRecorderHttp : HttpHandler {
    val calls = mutableListOf<Int>()

    private val answer = Path.int().of("answer")

    private val app = CatchLensFailure.then(
        routes(
            POST to "/{answer}" by { request -> calls.add(answer.extract(request)); Response(ACCEPTED) }
        )
    )

    override fun invoke(request: Request): Response = app(request)
}

interface RecorderContract {

    val client: HttpHandler

    @Test
    fun `records answer`() {
        client(Request(POST, "/123")).statusShouldBe(ACCEPTED)
        checkAnswerRecorded()
    }

    fun checkAnswerRecorded(): Unit {}

    @Test
    fun `non-numeric answer is bad request`() {
        client(Request(POST, "/notANumber")).statusShouldBe(BAD_REQUEST)
    }
}

class FakeRecorderTest : RecorderContract {
    override val client = FakeRecorderHttp()

    override fun checkAnswerRecorded() {
        client.calls shouldMatch equalTo(listOf(123))
    }
}

@Ignore // this obviously doesn't exist, so we ignore it here
class RealRecorderTest : RecorderContract {
    override val client = SetHostFrom(Uri.of("http://realrecorder")).then(OkHttp())
}

class EndToEndTest {
    private val port = 8000
    private val recorderPort = 10000
    private val client = OkHttp()
    private val recorder = FakeRecorderHttp()
    private val server = MyMathServer(port, Uri.of("http://localhost:$recorderPort"))
    private val recorderServer = recorder.asServer(Jetty(recorderPort))

    @Before
    fun setup(): Unit {
        recorderServer.start()
        server.start()
    }

    @After
    fun teardown(): Unit {
        server.stop()
        recorderServer.stop()
    }

    @Test
    fun `all endpoints are mounted correctly`() {
        client(Request(GET, "http://localhost:$port/ping")).statusShouldBe(OK)
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
        env.client(Request(GET, "/add?value=1&value=notANumber")).statusShouldBe(BAD_REQUEST)
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
        env.client(Request(GET, "/multiply?value=1&value=notANumber")).statusShouldBe(BAD_REQUEST)
        env.recorder.calls.isEmpty() shouldMatch equalTo(true)
    }
}

/** PRODUCTION **/

fun MyMathServer(port: Int, recorderBaseUri: Uri): Http4kServer =
    MyMathsApp(SetHostFrom(recorderBaseUri).then(OkHttp())).asServer(Jetty(port))

class Recorder(private val client: HttpHandler) {
    fun record(value: Int): Unit {
        client(Request(POST, "/$value"))
    }
}

fun MyMathsApp(recorderHttp: HttpHandler): HttpHandler {
    val recorder = Recorder(recorderHttp)
    return CatchLensFailure.then(
        routes(
            GET to "/ping" by { _: Request -> Response.Companion(OK) },
            GET to "/add" by calculate(recorder) { it.sum() },
            GET to "/multiply" by calculate(recorder) { it.fold(1) { memo, next -> memo * next } }
        )
    )
}

private fun calculate(recorder: Recorder, fn: (List<Int>) -> Int): (Request) -> Response {
    val values = Query.int().multi.defaulted("value", listOf())

    return {
        request: Request ->
        val valuesToCalc = values.extract(request)
        val answer = if (valuesToCalc.isEmpty()) 0 else fn(valuesToCalc)
        recorder.record(answer)
        Response(OK).body(answer.toString())
    }
}
