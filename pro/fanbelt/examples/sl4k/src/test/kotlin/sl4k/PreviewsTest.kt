package sl4k

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.Viewer
import sl4k.domain.PAGE
import sl4k.domain.PreviewId
import sl4k.domain.PreviewState.Pending
import sl4k.domain.PreviewState.Ready
import sl4k.domain.Unfurled
import sl4k.domain.Unfurler
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.feature.PreviewsFeature
import sl4k.jobs.PreviewJobs
import sl4k.store.Teams
import java.io.IOException
import java.time.Duration

class PreviewsTest {
    private val general = ChannelName.of("general")
    private val ann = Viewer(viewerNamed("ann"))

    private class Harness(sites: HttpHandler = fakeSites) {
        val fetched = mutableListOf<String>()
        val tenant = TestTenant(TestRuntime(http = { request: Request ->
            fetched += request.uri.toString()
            sites(request)
        }))
        val chat = ChatFeature(tenant.clock, tenant.random)
        val previews = PreviewsFeature(chat, tenant.clock, tenant.random)

        init {
            Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, ChannelName.of("general"))
            PreviewJobs(tenant.runtime, tenant.infra, chat).start()
        }

        fun rows() = tenant.scope.read(chat) { it.previews(ChannelName.of("general")) }.values.flatten()
        fun message() = tenant.read(chat.history, Window(ChannelName.of("general"), PAGE))!!.messages.single()
    }

    @Test
    fun `a fetch that fails is tried again at the next sweep`() {
        val failures = ArrayDeque(listOf(IOException("unreachable")))
        val app = Harness(sites = { request ->
            val failure = failures.removeFirstOrNull()
            if (failure != null) throw failure
            fakeSites(request)
        })
        app.tenant.execute(app.chat.post, ann, Draft(general, "https://example.test/article"))

        app.tenant.tick(Duration.ofSeconds(2))

        assertEquals(listOf("https://example.test/article", "https://example.test/article"), app.fetched)
        assertEquals(listOf(Ready), app.rows().map { it.state })
    }

    @Test
    fun `a link inside code isn't fetched`() {
        val app = Harness()

        app.tenant.execute(app.chat.post, ann, Draft(general, "run `curl https://example.test/article -s`"))
        app.tenant.tick(Duration.ofSeconds(2))

        assertEquals(emptyList<String>(), app.fetched)
    }

    @Test
    fun `a message's first three different links are fetched, once each`() {
        val app = Harness()
        val links = (1..4).map { "https://example.test/$it" }

        app.tenant.execute(app.chat.post, ann, Draft(general, "${links[0]} ${links[0]} ${links.drop(1).joinToString(" ")}"))
        app.tenant.tick(Duration.ofSeconds(2))

        assertEquals(links.take(3), app.fetched)
    }

    @Test
    fun `a page that can't be read, or has no title, is tried once and shows nothing`() {
        listOf("https://example.test/gone", "https://example.test/untitled").forEach { link ->
            val app = Harness()

            app.tenant.execute(app.chat.post, ann, Draft(general, link))
            app.tenant.tick(Duration.ofSeconds(5))

            assertEquals(listOf(link), app.fetched)
            assertEquals(emptyList<Any>(), app.rows())
        }
    }

    @Test
    fun `an outcome for a link that has since been edited away is ignored`() {
        val app = Harness()
        app.tenant.execute(app.chat.post, ann, Draft(general, "https://example.test/article"))
        val message = app.message().id
        app.tenant.execute(app.chat.edit, ann, Edit(general, message, "https://example.test/plain"))

        app.tenant.scope.execute(app.previews.unfurled, Unfurler, Unfurled(PreviewId(message, 0), "https://example.test/article", "Stale", ""))

        assertEquals(listOf(Pending to "https://example.test/plain"), app.rows().map { it.state to it.url })
    }
}
