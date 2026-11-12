package todo

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
import todo.feature.TodoFeature
import todo.store.Lists
import todo.web.HomeRoutes
import todo.web.TodoPage
import todo.web.TodoRoutes

fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node {
    val runtime = runtime.withEvents(
        EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events)
    )
    val infra = infrastructure(runtime)
    val lists = Lists(infra.storage)

    val todo = TodoFeature(runtime.random)
    val workspace = Workspace(infra, "/lists/{list}", lists::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()

    val oneList = poly(
        listOf(TodoRoutes(todo, workspace)),
        "/" bind TodoPage(workspace, todo, renderer),
    )

    val home = poly(HomeRoutes(lists, workspace, renderer, runtime.clock))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(oneList)))

    return Node(app, runtime, infra)
}