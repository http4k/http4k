package cookbook

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.by
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * This example shows the various styles of testing endpoints
 */

fun myMathsEndpoint(fn: (Int, Int) -> Int): HttpHandler = {
    req ->
    val answer = fn(req.query("first")!!.toInt(), req.query("second")!!.toInt())
    Response(OK).body("the answer is $answer")
}

class EndpointUnitTest {
    @Test
    fun `adds numbers`() {
        val unit = myMathsEndpoint { first, second -> first + second }
        val response = unit(Request(GET, "/").query("first", "123").query("second", "456"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("the answer is 579"))
    }
}

fun MyMathsApp(fn: (Int, Int) -> Int) =
    routes(
        GET to "/add" by myMathsEndpoint(fn)
    )

class ApplicationUnitTest {

    private val app = MyMathsApp { first, second -> first + second }

    @Test
    fun `adds numbers`() {
        val response = app(Request(GET, "/add").query("first", "123").query("second", "456"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("the answer is 579"))
    }

    @Test
    fun `not found`() {
        val response = app(Request(GET, "/nothing").query("first", "123").query("second", "456"))
        assertThat(response.status, equalTo(NOT_FOUND))
    }
}

fun MyMathServer(port: Int) = MyMathsApp { first, second -> first + second }.asServer(Jetty(port))

class ServerIntegrationTest {
    private val client = OkHttp()
    private val server = MyMathServer(8000)

    @Before
    fun setup(): Unit {
        server.start()
    }

    @After
    fun teardown(): Unit = server.stop()

    @Test
    fun `adds numbers`() {
        val response = client(Request(GET, "http://localhost:8000/add").query("first", "123").query("second", "456"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("the answer is 579"))
    }

}
