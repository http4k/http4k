package exchange.jobs

import exchange.domain.Actor
import exchange.domain.Price
import exchange.domain.Symbol
import exchange.feature.MarketFeature
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.events.Events
import org.http4k.fanbelt.events.FanbeltFailure
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.MILLISECONDS

data class FeedFailed(override val cause: Throwable) : FanbeltFailure

class FeedPoller(
    private val feed: HttpHandler,
    private val exchange: MarketFeature,
    private val scope: (TenantId) -> Scope,
    private val venues: () -> List<TenantId>,
    private val scheduler: ScheduledExecutorService,
    private val events: Events,
) : Lifecycle {
    @Volatile
    private var polling: ScheduledFuture<*>? = null

    override fun start() {
        polling = scheduler.scheduleWithFixedDelay(::poll, 0, 500, MILLISECONDS)
    }

    override fun close() {
        polling?.cancel(false)
    }

    private fun poll() {
        runCatching {
            val response = feed(Request(GET, "/prices"))
            check(response.status.successful) { "feed answered ${response.status}" }
            val prices = response.bodyString().lines().map { it.split(" ") }.associate { (symbol, price) -> Symbol.of(symbol) to Price.parse(price) }
            venues().forEach { scope(it).execute(exchange.mark, Actor.Feed, prices) }
        }.onFailure { events(FeedFailed(it)) }
    }
}