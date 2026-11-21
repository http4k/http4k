package sl4k.feature

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.filter.ClientFilters
import org.jsoup.Jsoup
import sl4k.domain.ChannelName
import sl4k.domain.MessageId
import sl4k.domain.Preview
import sl4k.domain.PreviewState.Pending
import sl4k.domain.Unfurled
import sl4k.domain.Unfurler
import sl4k.domain.Unreadable
import sl4k.domain.isWebLink
import sl4k.store.ChatTables

class PreviewsFeature(private val runtime: AppRuntime) : Feature<Chat, Unfurler>({ tables -> Chat(ChatTables(tables), runtime.clock, runtime.random) }) {
    val sites = ClientFilters.ReportHttpTransaction(runtime.events)
        .then(ClientFilters.FollowRedirects())
        .then(runtime.http)

    val previews by state { chat, channel: ChannelName -> chat.previews(channel) }

    val unfurled by operation { domain, _, found: Unfurled ->
        val preview = domain.unfurled(found) ?: return@operation Changes.none()
        Changes.of(previews.topic(preview.channel))
    }

    val unreadable by operation { domain, _, link: Unreadable ->
        domain.unreadable(link)
        Changes.none()
    }

    val linkPreviews by state { chat, message: MessageId -> chat.previewsOf(message) }

    val unfurl by job(linkPreviews) { scope, previews ->
        previews.filter { it.state == Pending }.forEach { fetch(scope, it) }
    }

    private fun fetch(scope: Scope, preview: Preview) {
        val page = sites(Request(GET, preview.url))
        val found = if (page.status.successful) metaOf(preview, page.bodyString()) else null
        if (found == null) scope.execute(unreadable, Unfurler, Unreadable(preview.id, preview.url))
        else scope.execute(unfurled, Unfurler, found)
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
