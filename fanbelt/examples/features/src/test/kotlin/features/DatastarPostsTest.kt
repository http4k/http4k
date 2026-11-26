package features

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@Timeout(10)
class DatastarPostsTest {
    private val server = App(AppRuntime()).asServer(Jetty(0)).start()
    private val http = JavaHttpClient()
    private val ann = viewerCookie(viewerNamed("ann"))

    init {
        http(Request(POST, server.uri().path("/spaces")).form("space", "acme"))
    }

    @AfterEach
    fun stop() {
        server.stop()
    }

    private fun datastarPost(path: String, signals: String) = http(
        Request(POST, server.uri().path("/spaces/acme/$path")).cookie(ann)
            .header("accept", "text/event-stream, text/html, application/json")
            .header("datastar-request", "true")
            .header("content-type", "application/json")
            .body(signals)
    )

    private fun page(path: String) = http(Request(GET, server.uri().path("/spaces/acme/$path")).cookie(ann)).bodyString()

    @Test
    fun `a note posted from the notes page reaches the operation, not the page's stream`() {
        datastarPost("notes", """{"note":"from the page"}""")

        assertThat(page("notes"), containsSubstring("from the page"))
    }

    @Test
    fun `a room created from the rooms page reaches the operation, not the page's stream`() {
        datastarPost("rooms", """{"name":"plans","private":false}""")

        assertThat(page("rooms"), containsSubstring("rooms/plans"))
    }
}
