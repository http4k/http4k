package sheets

import org.http4k.core.ContentEncoding
import org.http4k.core.patched.Brotli
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
import org.http4k.template.TemplateRenderer
import sheets.store.Sheets
import sheets.web.EnsureViewer
import sheets.web.identified
import sheets.web.HomeRoutes
import sheets.web.SheetPage
import sheets.web.SheetRoutes

fun App(
    runtime: AppRuntime = AppRuntime(),
    infrastructure: (AppRuntime) -> AppInfra = ::AppInfra,
    renderer: TemplateRenderer = HandlebarsTemplates().CachingClasspath(),
): Node {
    val runtime = runtime.withEvents(
        EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events))
    val infra = infrastructure(runtime)
    val sheets = Sheets(infra.storage)
    val workspace = Workspace(infra, "/sheets/{sheet}", sheets::exists)

    val oneSheet = poly(
        listOf(SheetRoutes(workspace)),
        "/" bind SheetPage(workspace, renderer),
    )

    val home = poly(EnsureViewer(runtime.random).then(HomeRoutes(sheets, workspace, renderer, runtime.clock)))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(identified.then(oneSheet))))

    return Node(app, runtime, infra)
}