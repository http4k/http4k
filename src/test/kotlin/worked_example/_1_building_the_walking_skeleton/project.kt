package worked_example._1_building_the_walking_skeleton

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class EndToEndTest {
    private val client = OkHttp()
    private val server = MyMathServer(8000)

    @Before
    fun setup(): Unit {
        server.start()
    }

    @After
    fun teardown(): Unit {
        server.stop()
    }

    @Test
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:8000/ping")).status shouldMatch equalTo(OK)
    }
}

fun MyMathServer(port: Int): Http4kServer = { _: Request -> Response(OK) }.asServer(Jetty(port))
