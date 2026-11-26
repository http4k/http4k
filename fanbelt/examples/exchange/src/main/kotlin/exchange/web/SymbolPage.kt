package exchange.web

import exchange.feature.MarketFeature
import exchange.view.SymbolPageView
import exchange.view.TicketSignals
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun SymbolPage(workspace: Workspace, exchange: MarketFeature, regions: ExchangeRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(
        listOf(regions.ticker, regions.book, regions.chart, regions.tape, regions.portfolio, regions.watching, regions.leaderboard),
        renderer,
        signals = TicketSignals(),
    ) { shell, (viewer, symbol) -> SymbolPageView(shell, symbol, viewer.id) },
    Viewing(principal, symbolPath) { scope, (viewer, symbol) -> exchange.watching.join(scope, symbol, viewer.id) },
)