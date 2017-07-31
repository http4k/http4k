package guide.example._1_building_a_walking_skeleton

import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class EndToEndTest {
    private val port = Random().nextInt(1000) + 8000
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
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:$port/ping")) shouldMatch hasStatus(OK)
    }
}
