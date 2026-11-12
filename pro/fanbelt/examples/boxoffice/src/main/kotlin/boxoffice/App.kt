package boxoffice

import boxoffice.feature.BoxOfficeFeature
import boxoffice.feature.OrdersFeature
import boxoffice.jobs.PaymentJobs
import boxoffice.store.Theatres
import boxoffice.web.BoxOfficeRoutes
import boxoffice.web.EnsureViewer
import boxoffice.web.identified
import boxoffice.web.ShowPage
import boxoffice.web.TheatrePage
import org.http4k.core.ContentEncoding
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.patched.Brotli
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
    val theatres = Theatres(infra.storage)
    val boxOffice = BoxOfficeFeature(runtime.clock)
    val orders = OrdersFeature(boxOffice, runtime.clock, runtime.random)
    val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()

    val oneTheatre = poly(
        listOf(BoxOfficeRoutes(boxOffice, orders, workspace)),
        "/shows/{show}" bind ShowPage(workspace, boxOffice, orders, renderer),
        "/" bind TheatrePage(workspace, boxOffice, renderer),
    )

    val home = poly(EnsureViewer(runtime.random).then("/" bind GET to { Response(SEE_OTHER).location(workspace.uri(TenantId.of("demo"))) }))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(identified.then(oneTheatre))))

    return Node(app, runtime, infra, PaymentJobs(runtime, infra, orders))
}