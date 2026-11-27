package sl4k.feature

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.filter.ClientFilters
import org.jsoup.Jsoup
import sl4k.domain.MessageId
import sl4k.domain.Preview
import sl4k.domain.PreviewState.Pending
import sl4k.domain.Unfurled
import sl4k.domain.Unfurler
import sl4k.domain.isWebLink
import sl4k.store.ChatTables

class PreviewsFeature(private val runtime: AppRuntime) : Feature<Chat, Unfurler>({ tables -> Chat(ChatTables(tables), runtime.clock, runtime.random) }) {
    val sites = ClientFilters.ReportHttpTransaction(runtime.events)
        .then(ClientFilters.FollowRedirects())
        .then(runtime.http)

    val previews by state(MessageId) { chat, message -> chat.previews(message) }

    val unfurled by operation { domain, _, found: Unfurled ->
        domain.unfurled(found) ?: return@operation Changes.none()
        Changes.of(previews.topic(found.message))
    }

    val unfurl by job(MessageId) { scope, id ->
        previews.read(scope, id)?.takeIf { it.state == Pending }?.let { unfurled.execute(scope, Unfurler, Unfurled(id, it.urls, it.urls.mapNotNull(::unfurl))) }
    }

    private fun unfurl(url: String): Preview? {
        val page = sites(Request(GET, url))
        return if (page.status.successful) metaOf(url, page.bodyString()) else null
    }

    private fun metaOf(url: String, html: String): Preview? {
        val page = Jsoup.parse(html, url)
        fun content(tag: String) = page.selectFirst(tag)?.attr("content").orEmpty()
        val title = content("meta[property=og:title]").ifBlank { page.title() }
        val description = content("meta[property=og:description]").ifBlank { content("meta[name=description]") }
        val image = page.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty().takeIf(::isWebLink).orEmpty()
        return if (title.isBlank()) null else Preview(url, title.take(200), description.take(300), image)
    }
}
