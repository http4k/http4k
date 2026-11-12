package helpdesk.web

import dev.forkhandles.values.parseOrNull
import helpdesk.domain.Org
import helpdesk.store.Orgs
import helpdesk.view.OrgLink
import helpdesk.view.OrgsPageView
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
import java.time.Clock

fun HomeRoutes(orgs: Orgs, workspace: Workspace, renderer: TemplateRenderer, clock: Clock): RoutingHttpHandler {
    val html = Body.viewModel(renderer, TEXT_HTML).toLens()
    fun home(status: Status, typed: String = "", error: String? = null) =
        Response(status).with(html of OrgsPageView(orgs.all().map { OrgLink(it.value, workspace.uri(it)) }, typed, error))

    return routes(
        "/" bind GET to { home(OK) },
        "/orgs" bind POST to { request ->
            val typed = request.form("org").orEmpty()
            when (val org = TenantId.parseOrNull(typed.trim())) {
                null -> home(BAD_REQUEST, typed, "An organisation name is letters, digits, - and _ (up to 100)")
                else -> when {
                    orgs.create(org, Org(clock.instant())) -> Response(SEE_OTHER).location(workspace.uri(org))
                    else -> home(CONFLICT, typed, "That name is taken")
                }
            }
        },
    )
}