package sheets.web

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
import sheets.domain.SheetDetails
import sheets.store.Sheets
import sheets.view.SheetLink
import sheets.view.SheetsPageView
import java.time.Clock

fun HomeRoutes(sheets: Sheets, workspace: Workspace, renderer: TemplateRenderer, clock: Clock): RoutingHttpHandler {
    val html = Body.viewModel(renderer, TEXT_HTML).toLens()
    fun home(status: Status, typed: String = "", error: String? = null) =
        Response(status).with(html of SheetsPageView(sheets.all().map { SheetLink(it.value, workspace.uri(it)) }, typed, error))

    return routes(
        "/" bind GET to { home(OK) },
        "/sheets" bind POST to { request ->
            val typed = request.form("sheet").orEmpty()
            when (val sheet = TenantId.parseOrNull(typed.trim())) {
                null -> home(BAD_REQUEST, typed, "A sheet name is letters, digits, - and _ (up to 100)")
                else -> when {
                    sheets.create(sheet, SheetDetails(clock.instant())) -> Response(SEE_OTHER).location(workspace.uri(sheet))
                    else -> home(CONFLICT, typed, "That name is taken")
                }
            }
        },
    )
}