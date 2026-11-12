package board

import board.feature.BoardFeature
import board.feature.BuildFeature
import board.store.Teams
import board.web.BoardPage
import board.web.BoardRoutes
import board.web.BuildRoutes
import board.web.EnsureViewer
import board.web.identified
import board.web.HomeRoutes
import org.http4k.core.ContentEncoding
import org.http4k.core.Brotli
import org.http4k.core.then
import org.http4k.events.EventFilters
import org.http4k.events.then
import org.http4k.filter.PolyFilters
import org.http4k.fanbelt.events.ReportFailures
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.poly
import org.http4k.template.HandlebarsTemplates

fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra, ciToken: String = "local-ci"): Node {
    val runtime = runtime.withEvents(
        EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events))
    val infra = infrastructure(runtime)
    val teams = Teams(infra.storage)
    val boards = BoardFeature(runtime.random)
    val ci = BuildFeature(runtime.random)
    val workspace = Workspace(infra, "/teams/{team}", teams::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()

    val oneTeam = poly(
        listOf(BoardRoutes(boards, workspace)),
        "/b/{board}" bind BoardPage(workspace, boards, ci, renderer),
    )

    val builds = workspace.routes(poly(BuildRoutes(ci, workspace, ciToken)))
    val home = poly(EnsureViewer(runtime.random).then(HomeRoutes(teams, workspace, renderer, runtime.clock)))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, builds, workspace.routes(identified.then(oneTeam))))

    return Node(app, runtime, infra)
}