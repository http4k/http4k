package exchange

import exchange.feature.MarketFeature
import exchange.jobs.FeedPoller
import exchange.store.Venues
import exchange.web.EnsureViewer
import exchange.web.identified
import exchange.web.ExchangeRegions
import exchange.web.ExchangeRoutes
import exchange.web.SymbolPage
import exchange.web.VenuePage
import org.http4k.core.ContentEncoding
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Brotli
import org.http4k.core.then
import org.http4k.events.EventFilters
import org.http4k.events.then
import org.http4k.filter.PolyFilters
import org.http4k.lens.location
import org.http4k.fanbelt.events.ReportFailures
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.template.HandlebarsTemplates

fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node {
    val runtime = runtime.withEvents(EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events))
    val infra = infrastructure(runtime)
    val venues = Venues(infra.storage)
    val exchange = MarketFeature(runtime.clock, runtime.random)
    val workspace = Workspace(infra, "/venues/{venue}", venues::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()
    val regions = ExchangeRegions(exchange, renderer)

    val oneVenue = poly(
        listOf(ExchangeRoutes(exchange, workspace)),
        "/s/{symbol}" bind SymbolPage(workspace, exchange, regions, renderer),
        "/" bind VenuePage(workspace, regions, renderer),
    )

    val home = poly(EnsureViewer(runtime.random).then("/" bind GET to { Response(SEE_OTHER).location(workspace.uri(TenantId.of("demo"))) }))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(identified.then(oneVenue))))

    return Node(app, runtime, infra, FeedPoller(runtime.http, exchange, infra::scope, venues::all, runtime.scheduler, runtime.events))
}