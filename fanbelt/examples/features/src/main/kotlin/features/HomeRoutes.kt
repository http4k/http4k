package features

import dev.forkhandles.values.parseOrNull
import java.time.Clock
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
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.lens.location
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

fun HomeRoutes(spaces: Spaces, workspace: Workspace, renderer: TemplateRenderer, clock: Clock): RoutingHttpHandler {
    val html = Body.viewModel(renderer, TEXT_HTML).toLens()
    fun home(status: Status, typed: String = "", error: String? = null) =
        Response(status).with(html of SpacesPageView(spaces.all().map { SpaceLink(it.value, workspace.uri(it)) }, typed, error))

    return routes(
        "/" bind GET to { home(OK) },
        "/spaces" bind POST to { request ->
            val typed = request.form("space").orEmpty()
            when (val space = TenantId.parseOrNull(typed.trim())) {
                null -> home(BAD_REQUEST, typed, "A space name is letters, digits, - and _ (up to 100)")
                else -> when {
                    spaces.create(space, SpaceDetails(clock.instant())) -> Response(SEE_OTHER).location(workspace.uri(space))
                    else -> home(CONFLICT, typed, "That name is taken")
                }
            }
        },
    )
}

data class SpaceLink(val name: String, val href: Uri)

data class SpacesPageView(val spaces: List<SpaceLink>, val typed: String, val error: String?) : ViewModel