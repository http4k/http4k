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

/**
 * 1. Building the Walking Skeleton
 * Until we have an application that can be deployed, we cannot create any business value. The Walking Skeleton
 * model dictates that putting the most trivial endpoint into a production environment will prove our deployment
 * pipeline is sound, and helps to set the direction for the testing strategy that we will use going forward.
 *
 * We start with in ICT (In-Container-Test), which have the job of testing server-level concerns such as monitoring,
 * documentation, and checking in a high-level way that the business endpoints are wired correctly.
 *
 * REQUIREMENTS:
 * - The service can be pinged over HTTP to prove that is still alive.
 */

/** TESTS **/
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
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:$port/ping")).status shouldMatch equalTo(OK)
    }
}

/** PRODUCTION **/
fun MyMathServer(port: Int): Http4kServer = { _: Request -> Response(OK) }.asServer(Jetty(port))