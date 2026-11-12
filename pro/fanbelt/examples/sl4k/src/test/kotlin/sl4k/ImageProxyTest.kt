package sl4k

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.start
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
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

        assertEquals(OK, response.status)
        assertEquals("image/png", response.header("content-type"))
        assertEquals("nosniff", response.header("x-content-type-options"))
        assertEquals("sandbox", response.header("content-security-policy"))
        assertEquals("private, max-age=86400", response.header("cache-control"))
        assertArrayEquals(tinyPng, response.body.payload.array())
    }

    @Test
    fun `the site never learns who is reading`() {
        val app = Harness()

        app.posted("https://example.test/pictured")

        val toTheImage = app.fetched.last()
        assertEquals("https://example.test/hero.png", toTheImage.uri.toString())
        assertEquals(emptyList<Pair<String, String?>>(), toTheImage.headers)
    }

    @Test
    fun `svg is not an image we will serve`() {
        assertEquals(NOT_FOUND, Harness().posted("https://example.test/vector").status)
    }

    @Test
    fun `an image the site can't produce is not found`() {
        assertEquals(NOT_FOUND, Harness().posted("https://example.test/broken").status)
    }

    @Test
    fun `a page with no image has nothing to proxy`() {
        val app = Harness()

        assertEquals(NOT_FOUND, app.posted("https://example.test/article").status)
        assertEquals(listOf("https://example.test/article"), app.fetched.map { it.uri.toString() })
    }

    @Test
    fun `an image belongs to the channel its message is in`() {
        val app = Harness()
        app.posted("https://example.test/pictured")
        app.post("channels", """{"name":"other"}""")

        assertEquals(NOT_FOUND, app.image(otherChannel = "other").status)
    }

    @Test
    fun `someone outside a private channel can't fetch its images`() {
        val app = Harness()
        app.post("channels", """{"name":"secret","private":true}""")

        assertEquals(OK, app.posted("https://example.test/pictured", "secret").status)
        assertEquals(NOT_FOUND, app.image("secret", bob).status)
    }

    @Test
    fun `each link's image has its own address`() {
        val app = Harness()

        app.posted("https://example.test/article https://example.test/pictured")

        assertEquals(NOT_FOUND, app.chat.get("c/general/previews/${app.message()}/0/image", ann).status)
        assertEquals(OK, app.chat.get("c/general/previews/${app.message()}/1/image", ann).status)
    }
}
