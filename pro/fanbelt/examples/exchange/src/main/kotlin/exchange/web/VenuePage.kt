package exchange.web

import exchange.view.TicketSignals
import exchange.view.VenuePageView
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun VenuePage(workspace: Workspace, regions: ExchangeRegions, renderer: TemplateRenderer) =
    Page(workspace, Layout(listOf(regions.ticker, regions.leaderboard, regions.portfolio), renderer, signals = TicketSignals()) { request, shell ->
        VenuePageView(shell, principal(request).id)
    })