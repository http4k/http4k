package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Test
import sl4k.web.viewerCookie
import java.time.Duration

class ImageProxyTest {
    private val team = TenantId.of("acme")
    private val ann = viewerCookie(viewerNamed("ann"))
    private val bob = viewerCookie(viewerNamed("bob"))

    private inner class Harness {
        val fetched = mutableListOf<Request>()
        val chat = ChatApp({ request ->
            fetched += request
            fakeSites(request)
        }).apply { node.start() }
        val http = chat.http

        fun post(path: String, body: String, cookie: Cookie = ann) = chat.post(path, cookie, body)

        fun posted(link: String, channel: String = "general"): Response {
            post("c/$channel/messages", """{"text":"$link"}""")
            chat.scheduler.tick(Duration.ofSeconds(2))
            chat.scheduler.runUntilIdle()
            return image(channel)
        }

        fun image(channel: String = "general", cookie: Cookie = ann, otherChannel: String = channel) =
            chat.get("c/$otherChannel/previews/${message()}/0/image", cookie)

        fun message() = chat.storage.chat { it.messages.page().items.single().id }
    }

    @Test
    fun `the image is served from our own origin, locked down`() {
        val response = Harness().posted("https://example.test/pictured")

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("content-type"), equalTo("image/png"))
        assertThat(response.header("x-content-type-options"), equalTo("nosniff"))
        assertThat(response.header("content-security-policy"), equalTo("sandbox"))
        assertThat(response.header("cache-control"), equalTo("private, max-age=86400"))
        assertThat(response.body.payload.array().toList(), equalTo(tinyPng.toList()))
    }

    @Test
    fun `the site never learns who is reading`() {
        val app = Harness()

        app.posted("https://example.test/pictured")

        val toTheImage = app.fetched.last()
        assertThat(toTheImage.uri.toString(), equalTo("https://example.test/hero.png"))
        assertThat(toTheImage.headers, equalTo(emptyList<Pair<String, String?>>()))
    }

    @Test
    fun `svg is not an image we will serve`() {
        assertThat(Harness().posted("https://example.test/vector").status, equalTo(NOT_FOUND))
    }

    @Test
    fun `an image the site can't produce is not found`() {
        assertThat(Harness().posted("https://example.test/broken").status, equalTo(NOT_FOUND))
    }

    @Test
    fun `a page with no image has nothing to proxy`() {
        val app = Harness()

        assertThat(app.posted("https://example.test/article").status, equalTo(NOT_FOUND))
        assertThat(app.fetched.map { it.uri.toString() }, equalTo(listOf("https://example.test/article")))
    }

    @Test
    fun `an image belongs to the channel its message is in`() {
        val app = Harness()
        app.posted("https://example.test/pictured")
        app.post("channels", """{"name":"other"}""")

        assertThat(app.image(otherChannel = "other").status, equalTo(NOT_FOUND))
    }

    @Test
    fun `someone outside a private channel can't fetch its images`() {
        val app = Harness()
        app.post("channels", """{"name":"secret","private":true}""")

        assertThat(app.posted("https://example.test/pictured", "secret").status, equalTo(OK))
        assertThat(app.image("secret", bob).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `each link's image has its own address`() {
        val app = Harness()

        app.posted("https://example.test/article https://example.test/pictured")

        assertThat(app.chat.get("c/general/previews/${app.message()}/0/image", ann).status, equalTo(NOT_FOUND))
        assertThat(app.chat.get("c/general/previews/${app.message()}/1/image", ann).status, equalTo(OK))
    }
}
