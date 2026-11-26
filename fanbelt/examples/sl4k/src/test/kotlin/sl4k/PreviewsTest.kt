package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.PAGE
import sl4k.domain.Preview
import sl4k.domain.PreviewState.Ready
import sl4k.domain.Unfurled
import sl4k.domain.Unfurler
import sl4k.domain.Viewer
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams
import java.io.IOException
import java.time.Duration

class PreviewsTest {
    private val general = ChannelName.of("general")
    private val ann = Viewer(viewerNamed("ann"))

    private class Harness(sites: HttpHandler = fakeSites) {
        val fetched = mutableListOf<String>()
        val runtime = TestRuntime(http = { request: Request ->
            fetched += request.uri.toString()
            sites(request)
        })
        val chat = ChatFeature(runtime)
        val previews = chat.unfurling
        val tenant = TestTenant(runtime, jobs = listOf(previews.unfurl))

        init {
            Teams(tenant.storage).create(tenant.id, tenant.clock, ChannelName.of("general"))
        }

        fun row() = tenant.read(previews.previews, message().id)
        fun rows() = row()?.links.orEmpty()
        fun message() = tenant.read(chat.history, Window(ChannelName.of("general"), PAGE))!!.messages.single()
    }

    @Test
    fun `a fetch that fails is tried again a second later`() {
        val failures = ArrayDeque(listOf(IOException("unreachable")))
        val app = Harness(sites = { request ->
            val failure = failures.removeFirstOrNull()
            if (failure != null) throw failure
            fakeSites(request)
        })
        app.tenant.execute(app.chat.post, ann, Draft(general, "https://example.test/article"))
        app.tenant.tick(Duration.ofMillis(999))
        val failedOnce = app.fetched.toList()

        app.tenant.tick(Duration.ofMillis(1))

        assertThat(failedOnce, equalTo(listOf("https://example.test/article")))
        assertThat(app.fetched, equalTo(listOf("https://example.test/article", "https://example.test/article")))
        assertThat(app.row()?.state, equalTo(Ready))
    }

    @Test
    fun `a link inside code isn't fetched`() {
        val app = Harness()

        app.tenant.execute(app.chat.post, ann, Draft(general, "run `curl https://example.test/article -s`"))
        app.tenant.tick(Duration.ofSeconds(2))

        assertThat(app.fetched, equalTo(emptyList<String>()))
    }

    @Test
    fun `a message's first three different links are fetched as soon as it's posted, once each`() {
        val app = Harness()
        val links = (1..4).map { "https://example.test/$it" }

        app.tenant.execute(app.chat.post, ann, Draft(general, "${links[0]} ${links[0]} ${links.drop(1).joinToString(" ")}"))

        assertThat(app.fetched, equalTo(links.take(3)))
    }

    @Test
    fun `a page that can't be read, or has no title, is tried once and shows nothing`() {
        listOf("https://example.test/gone", "https://example.test/untitled").forEach { link ->
            val app = Harness()

            app.tenant.execute(app.chat.post, ann, Draft(general, link))
            app.tenant.tick(Duration.ofSeconds(5))

            assertThat(app.fetched, equalTo(listOf(link)))
            assertThat(app.rows(), equalTo(emptyList<Any>()))
        }
    }

    @Test
    fun `an outcome for a link that has since been edited away is ignored`() {
        val app = Harness()
        app.tenant.execute(app.chat.post, ann, Draft(general, "https://example.test/article"))
        val message = app.message().id
        app.tenant.execute(app.chat.edit, ann, Edit(general, message, "https://example.test/plain"))

        app.tenant.execute(app.previews.unfurled, Unfurler, Unfurled(message, listOf("https://example.test/article"), listOf(Preview("https://example.test/article", "Stale"))))

        assertThat(app.rows().map { it.url to it.title }, equalTo(listOf("https://example.test/plain" to "A plain page")))
    }
}
