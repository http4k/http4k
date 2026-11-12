package todo.web

import dev.forkhandles.values.parseOrNull
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.location
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.viewModel
import todo.domain.TodoList
import todo.store.Lists
import todo.view.ListLink
import todo.view.ListsPageView
import java.time.Clock

fun HomeRoutes(lists: Lists, workspace: Workspace, renderer: TemplateRenderer, clock: Clock): RoutingHttpHandler {
    val html = Body.viewModel(renderer, TEXT_HTML).toLens()
    fun home(status: Status, typed: String = "", error: String? = null) =
        Response(status).with(html of ListsPageView(lists.all().map { ListLink(it.value, workspace.uri(it)) }, typed, error))

    return routes(
        "/" bind GET to { home(OK) },
        "/lists" bind POST to { request ->
            val typed = request.form("list").orEmpty()
            when (val created = TenantId.parseOrNull(typed.trim())) {
                null -> home(BAD_REQUEST, typed, "A list name is letters, digits, - and _ (up to 100)")
                else -> when {
                    lists.create(created, TodoList(clock.instant())) -> Response(SEE_OTHER).location(workspace.uri(created))
                    else -> home(CONFLICT, typed, "That name is taken")
                }
            }
        },
    )
}