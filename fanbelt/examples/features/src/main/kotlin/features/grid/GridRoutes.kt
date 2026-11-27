package features.grid

import features.principal
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.fanbelt.feature.invoke
import org.http4k.fanbelt.state.carry
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun GridRoutes(workspace: Workspace, grid: GridFeature): RoutingHttpHandler {
    val viewer = { request: Request -> principal(request).id }

    return routes(
        "/cells/{cell}" bind POST to grid.set(workspace, viewer, cellRef) { signals: EditSignals, cell -> cell to signals.edit },
        "/grid.csv" bind GET to grid.cells(workspace, viewer) { cells ->
            Response(OK).header("content-type", "text/csv").body("cell,text\n" + cells.joinToString("") { "${it.id},${csv(it.text.value)}\n" })
        },
        "/cursor/{cell}" bind POST to grid.here.carry(workspace, viewer) { cellRef(it) },
    )
}

private fun csv(text: String) = if (text.any { it in ",\"\n" }) "\"${text.replace("\"", "\"\"")}\"" else text
