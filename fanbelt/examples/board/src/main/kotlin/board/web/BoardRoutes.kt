package board.web

import board.domain.BuildPosted
import board.domain.CardMove
import board.domain.Ci
import board.domain.RoleChange
import board.feature.BoardFeature
import board.feature.BuildFeature
import board.store.BoardJson.json
import board.view.CardSignals
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.appendToPath
import org.http4k.core.then
import org.http4k.fanbelt.input.Statuses
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.location
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun BoardRoutes(boards: BoardFeature, workspace: Workspace): RoutingHttpHandler {
    val me = { request: Request -> principal(request).id }

    return routes(
        "/" bind GET to { request -> Response(SEE_OTHER).location(workspace.uri(request).appendToPath("b/lobby")) },
        "/b/{board}/join" bind POST to boards.join(workspace, me, input = boardId),
        "/b/{board}/cards" bind POST to boards.add(
            workspace, me, { signals: CardSignals, request -> boardId(request) to signals.text },
            reset = setOf(CardSignals::text),
        ),
        "/b/{board}/cards/{card}/move/{direction}" bind POST to boards.move(workspace, me) {
            CardMove(boardId(it), cardId(it), direction(it))
        },
        "/b/{board}/cards/{card}/delete" bind POST to boards.delete(workspace, me) { boardId(it) to cardId(it) },
        "/b/{board}/members/{viewer}/role/{role}" bind POST to boards.setRole(workspace, me) {
            RoleChange(boardId(it), viewerId(it), role(it))
        },
    )
}

fun BuildRoutes(ci: BuildFeature, workspace: Workspace, ciToken: String) = routes(
    "/b/{board}/builds" bind POST to CatchLensFailure().then(ServerFilters.BearerAuth(ciToken)).then(
        ci.report(workspace, { Ci }, Statuses()) { boardId(it) to it.json<BuildPosted>() }
    ),
)
