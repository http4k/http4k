package cookbook.test_driven_apps

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnswerRecorder(private val httpClient: HttpHandler) : (Int) -> Unit {
    override fun invoke(answer: Int) {
        httpClient(Request(POST, "/$answer"))
    }
}

fun myMathsEndpoint(fn: (Int, Int) -> Int, recorder: (Int) -> Unit) = HttpHandler { req ->
    val answer = fn(req.query("first")!!.toInt(), req.query("second")!!.toInt())
    recorder(answer)
    Response(OK).body("the answer is $answer")
}

class EndpointUnitTest {
    @Test
    fun `adds numbers and records answer`() {
        var answer: Int? = null
        val unit = myMathsEndpoint({ first, second -> first + second }, { answer = it })
        val response = unit(Request(GET, "/").query("first", "123").query("second", "456"))
        assertThat(answer, equalTo(579))
        assertThat(response, hasStatus(OK).and(hasBody("the answer is 579")))
    }
}

fun MyMathsApp(recorderHttp: HttpHandler) =
    ServerFilters.CatchAll().then(routes(
        "/add" bind GET to myMathsEndpoint({ first, second -> first + second }, AnswerRecorder(recorderHttp))
    ))

class FakeRecorderHttp : HttpHandler {
    val calls = mutableListOf<Int>()

    private val app = routes(
        "/{answer}" bind POST to { request -> calls.add(request.path("answer")!!.toInt()); Response(OK) }
    )

    override fun invoke(request: Request): Response = app(request)
}

class FunctionalTest {

    private val recorderHttp = FakeRecorderHttp()
    private val app = MyMathsApp(recorderHttp)

    @Test
    fun `adds numbers`() {
        val response = app(Request(GET, "/add").query("first", "123").query("second", "456"))
        assertThat(response, hasStatus(OK).and(hasBody("the answer is 579")))
        assertThat(recorderHttp.calls, equalTo(listOf(579)))
    }

    @Test
    fun `not found`() {
        val response = app(Request(GET, "/nothing").query("first", "123").query("second", "456"))
        assertThat(response, hasStatus(NOT_FOUND))
    }
}

fun MyMathServer(port: Int, recorderUri: Uri): Http4kServer {
    val recorderHttp = SetHostFrom(recorderUri).then(OkHttp())
    return MyMathsApp(recorderHttp).asServer(Jetty(port))
}

class EndToEndTest {
    private val client = OkHttp()
    private val recorderHttp = FakeRecorderHttp()
    private val recorder = recorderHttp.asServer(Jetty(8001))
    private val server = MyMathServer(8000, Uri.of("http://localhost:8001"))

    @BeforeEach
    fun setup() {
        recorder.start()
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.stop()
        recorder.stop()
    }

    @Test
    fun `adds numbers`() {
        val response = client(Request(GET, "http://localhost:8000/add").query("first", "123").query("second", "456"))
        println(response)
        assertThat(response, hasStatus(OK).and(hasBody("the answer is 579")))
        assertThat(recorderHttp.calls, equalTo(listOf(579)))
    }

}
