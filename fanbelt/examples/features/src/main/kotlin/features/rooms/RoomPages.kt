package features.rooms

import features.ViewerId
import features.Viewer
import features.principal
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.appendToPath
import org.http4k.fanbelt.input.GoTo
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

val roomName = Path.value(RoomName).of("room")

private val viewer = { request: Request -> principal(request).id }

data class NewRoomSignals(val name: String = "", val private: Boolean = false)

data class RoomSignals(val line: String = "", val title: String = "")

data class RoomLink(val name: String, val title: String, val private: Boolean)

data class RoomsView(val rooms: List<RoomLink>) : ViewModel

data class RoomsPageView(val shell: PageShell) : ViewModel

data class TitleView(val title: String) : ViewModel

data class RoomHereView(val names: List<String>) : ViewModel

data class LineRow(val by: String, val text: String)

data class LinesView(val lines: List<LineRow>) : ViewModel

data class RoomPageView(val shell: PageShell, val room: String, val unseen: Int, val yours: Boolean) : ViewModel

fun RoomsPage(workspace: Workspace, rooms: RoomsFeature, renderer: TemplateRenderer): Page {
    val list = Region("rooms", renderer, rooms.rooms) { all, me: ViewerId ->
        RoomsView(all.filter { it.visibleTo(me) }.map { RoomLink(it.id.value, it.title.value, it.private) })
    }
    return Page(workspace, Layout(listOf(list), renderer, signals = NewRoomSignals()) { shell -> RoomsPageView(shell) }, Viewing(viewer))
}

fun RoomPage(workspace: Workspace, rooms: RoomsFeature, renderer: TemplateRenderer): Page {
    val title = Region("title", renderer, roomName, rooms.room) { room, _ -> TitleView(room?.title?.value.orEmpty()) }
    val here = Region("here", renderer, roomName, rooms.here) { present, _ -> RoomHereView(present.keys.map { Viewer(it).name }.sorted()) }
    val lines = Region("lines", renderer, roomName, rooms.lines) { said, _ -> LinesView(said.map { LineRow(Viewer(it.by).name, it.text.value) }) }
    return Page(
        workspace,
        Layout(listOf(title, here, lines), renderer, subject = title, signals = RoomSignals()) { scope, shell, (me, name, room) ->
            RoomPageView(shell, name.value, rooms.unseen.read(scope, Reader(name, me)), room.owner == me)
        },
        Viewing(viewer, roomName, rooms.visibleRoom, { workspace.uri(it).appendToPath("rooms") }, rooms.enter),
    )
}

fun RoomRoutes(workspace: Workspace, rooms: RoomsFeature) = routes(
    "/rooms" bind POST to rooms.create(workspace, viewer, feedback = GoTo { Uri.of("rooms/$it") }) { signals: NewRoomSignals -> NewRoom(signals.name, signals.private) },
    "/rooms/{room}/say" bind POST to rooms.say(workspace, viewer, roomName, reset = setOf(RoomSignals::line)) { signals: RoomSignals, room -> Said(room, signals.line) },
    "/rooms/{room}/rename" bind POST to rooms.rename(workspace, viewer, roomName, reset = setOf(RoomSignals::title)) { signals: RoomSignals, room -> Rename(room, signals.title) },
)
