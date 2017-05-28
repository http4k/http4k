package worked_example._3_adding_another_endpoint

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.junit.After
import org.junit.Before
import org.junit.Test
import worked_example._3_adding_another_endpoint.Matchers.answerShouldBe
import worked_example._3_adding_another_endpoint.Matchers.statusShouldBe

object Matchers {
    fun Response.statusShouldBe(expected: Status) = status shouldMatch equalTo(expected)

    fun Response.answerShouldBe(expected: Int) {
        statusShouldBe(OK)
        bodyString().toInt() shouldMatch equalTo(expected)
    }
}

class EndToEndTest {
    private val port = 8000
    private val client = OkHttp()
    private val server = MyMathServer(port)

    @Before
    fun setup(): Unit {
        server.start()
    }

    @After
    fun teardown(): Unit {
        server.stop()
    }

    @Test
    fun `all endpoints are mounted correctly`() {
        client(Request(Method.GET, "http://localhost:$port/ping")).statusShouldBe(OK)
        client(Request(Method.GET, "http://localhost:$port/add?value=1&value=2")).answerShouldBe(3)
        client(Request(Method.GET, "http://localhost:$port/multiply?value=2&value=4")).answerShouldBe(8)
    }
}

class AddFunctionalTest {
    private val client = MyMathsApp()

    @Test
    fun `adds values together`() {
        client(Request(Method.GET, "/add?value=1&value=2")).answerShouldBe(3)
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(Method.GET, "/add")).answerShouldBe(0)
    }

    @Test
    fun `bad request when some values are not numbers`() {
        client(Request(Method.GET, "/add?value=1&value=notANumber")).statusShouldBe(BAD_REQUEST)
    }
}

class MultiplyFunctionalTest {
    private val client = MyMathsApp()

    @Test
    fun `products values together`() {
        client(Request(Method.GET, "/multiply?value=2&value=4")).answerShouldBe(8)
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(Method.GET, "/multiply")).answerShouldBe(0)
    }

    @Test
    fun `bad request when some values are not numbers`() {
        client(Request(Method.GET, "/multiply?value=1&value=notANumber")).statusShouldBe(BAD_REQUEST)
    }
}