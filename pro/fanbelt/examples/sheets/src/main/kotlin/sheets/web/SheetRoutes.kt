package sheets.web

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.fanbelt.state.set
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import sheets.feature.SheetFeature
import sheets.view.EditSignals

fun SheetRoutes(workspace: Workspace): RoutingHttpHandler {
    val actor = { request: Request -> principal(request).id }

    return routes(
        "/cells/{cell}" bind POST to SheetFeature.set(workspace, actor, { signals: EditSignals, request -> cellRef(request) to signals.edit }),
        "/cursor/{cell}" bind POST to SheetFeature.here.set(workspace, actor) { cellRef(it) },
    )
}