package sl4k.jobs

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.fanbelt.events.FanbeltFailure
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.tenancy.Scope
import org.jsoup.Jsoup
import sl4k.domain.Preview
import sl4k.domain.PreviewState.Pending
import sl4k.domain.Unfurled
import sl4k.domain.Unfurler
import sl4k.domain.Unreadable
import sl4k.domain.isWebLink
import sl4k.feature.ChatFeature
import sl4k.feature.PreviewsFeature
import sl4k.store.ChatTables
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS

data class PreviewSweepFailed(override val cause: Throwable) : FanbeltFailure

class PreviewJobs(private val runtime: AppRuntime, private val infra: AppInfra, chat: ChatFeature) : Lifecycle {
    private val previews = PreviewsFeature(chat, runtime.clock, runtime.random)
    val sites = ClientFilters.ReportHttpTransaction(runtime.events)
        .then(ClientFilters.FollowRedirects())
        .then(runtime.http)

    @Volatile
    private var sweeping: ScheduledFuture<*>? = null

    override fun start() {
        sweeping = runtime.scheduler.scheduleWithFixedDelay(::sweep, 1, 1, SECONDS)
    }

    override fun close() {
        sweeping?.cancel(false)
    }

    private fun sweep() {
        runCatching {
            infra.storage.read { it.having(ChatTables.PREVIEWS, Preview::state eq Pending) }.forEach { unfurl(infra.scope(it)) }
        }.onFailure { runtime.events(PreviewSweepFailed(it)) }
    }

    private fun unfurl(scope: Scope) = scope.read(previews) { it.pendingPreviews() }.forEach { fetch(scope, it) }

    private fun fetch(scope: Scope, preview: Preview) {
        val page = sites(Request(GET, preview.url))
        val found = if (page.status.successful) metaOf(preview, page.bodyString()) else null
        if (found == null) scope.execute(previews.unreadable, Unfurler, Unreadable(preview.id, preview.url))
        else scope.execute(previews.unfurled, Unfurler, found)
    }

    private fun metaOf(preview: Preview, html: String): Unfurled? {
        val page = Jsoup.parse(html, preview.url)
        fun content(tag: String) = page.selectFirst(tag)?.attr("content").orEmpty()
        val title = content("meta[property=og:title]").ifBlank { page.title() }
        val description = content("meta[property=og:description]").ifBlank { content("meta[name=description]") }
        val image = page.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty().takeIf(::isWebLink).orEmpty()
        return if (title.isBlank()) null else Unfurled(preview.id, preview.url, title.take(200), description.take(300), image)
    }
}
