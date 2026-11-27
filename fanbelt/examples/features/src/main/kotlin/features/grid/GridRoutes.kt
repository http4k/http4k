package features.grid

import features.principal
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun GridRoutes(workspace: Workspace, grid: GridFeature): RoutingHttpHandler {
    val viewer = { request: Request -> principal(request).id }

    return routes(
        "/cells/{cell}" bind POST to workspace.execute(grid.set, viewer, cellRef) { signals: EditSignals, cell -> cell to signals.edit },
        "/grid.csv" bind GET to workspace.read(grid.cells, viewer) { cells ->
            Response(OK).header("content-type", "text/csv").body("cell,text\n" + cells.joinToString("") { "${it.id},${csv(it.text.value)}\n" })
        },
        "/cursor/{cell}" bind POST to workspace.carry(grid.here, viewer) { cellRef(it) },
    )
}

private fun csv(text: String) = if (text.any { it in ",\"\n" }) "\"${text.replace("\"", "\"\"")}\"" else text
