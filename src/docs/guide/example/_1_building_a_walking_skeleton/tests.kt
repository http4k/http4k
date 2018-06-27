package guide.example._1_building_a_walking_skeleton

import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class EndToEndTest {
    private val port = Random().nextInt(1000) + 8000
    private val client = OkHttp()
    private val server = MyMathServer(port)

    @BeforeEach
    fun setup(): Unit {
        server.start()
    }

    @AfterEach
    fun teardown(): Unit {
        server.stop()
    }

    @Test
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:$port/ping")) shouldMatch hasStatus(OK)
    }
}
