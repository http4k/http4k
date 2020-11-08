package blog.tdding_http4k._2

import blog.tdding_http4k._2.Matchers.answerShouldBe
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

object Matchers {
    fun Response.answerShouldBe(expected: Int) {
        assertThat(this, hasStatus(OK).and(hasBody(expected.toString())))
    }
}

class EndToEndTest {
    private val client = OkHttp()
    private val server = MyMathServer(0)

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.stop()
    }

    @Test
    fun `all endpoints are mounted correctly`() {
        assertThat(client(Request(GET, "http://localhost:${server.port()}/ping")), hasStatus(OK))
        client(Request(GET, "http://localhost:${server.port()}/add?value=1&value=2")).answerShouldBe(3)
    }
}

class AddFunctionalTest {
    private val client = MyMathsApp()

    @Test
    fun `adds values together`() {
        client(Request(GET, "/add?value=1&value=2")).answerShouldBe(3)
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(GET, "/add")).answerShouldBe(0)
    }

    @Test
    fun `bad request when some values are not numbers`() {
        assertThat(client(Request(GET, "/add?value=1&value=notANumber")), hasStatus(BAD_REQUEST))
    }
}
