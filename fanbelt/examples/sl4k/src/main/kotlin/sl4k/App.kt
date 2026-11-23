package sl4k

import org.http4k.core.Brotli
import org.http4k.core.ContentEncoding
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.fanbelt.auth.Authenticated
import org.http4k.fanbelt.events.ReportFailures
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.job.Jobs
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.filter.PolyFilters
import org.http4k.lens.location
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import sl4k.domain.Viewer
import sl4k.feature.ChatFeature
import sl4k.store.Teams
import sl4k.web.ChatRoutes
import sl4k.web.EnsureViewer
import sl4k.web.principal
import sl4k.web.viewerId

fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node {
    val infra = infrastructure(runtime)
    val teams = Teams(infra.storage)
    val chat = ChatFeature(runtime)
    val workspace = Workspace(infra, "/teams/{team}", teams::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()
    val jobs = Jobs(infra, chat.unfurling.unfurl)
    val oneTeam = ChatRoutes(chat, workspace, renderer, chat.unfurling.sites)

    val home = poly(
        "/public" bind static(ResourceLoader.Classpath("public")),
        EnsureViewer(runtime.random).then("/" bind GET to { Response(SEE_OTHER).location(workspace.uri(TenantId.of("demo"))) }),
    )

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(Authenticated(principal, refused) { it.viewerId()?.let(::Viewer) }.then(oneTeam))))

    return Node(app, runtime, infra, jobs)
}

private val refused: HttpHandler = { Response(SEE_OTHER).location(Uri.of("/")) }
