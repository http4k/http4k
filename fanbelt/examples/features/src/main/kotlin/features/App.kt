package features

import features.builds.BuildsFeature
import features.builds.BuildsPage
import features.builds.BuildsWebhook
import features.notes.NotesFeature
import features.notes.NotesPage
import features.notes.NotesRoutes
import features.rooms.AnnouncementsFeature
import features.rooms.RoomPage
import features.rooms.RoomRoutes
import features.rooms.RoomsFeature
import features.rooms.RoomsPage
import org.http4k.fanbelt.job.Jobs
import features.status.StatusFeature
import features.status.StatusPage
import org.http4k.core.Uri
import org.http4k.filter.ClientFilters
import features.grid.GridFeature
import features.grid.GridPage
import features.grid.GridRoutes
import org.http4k.core.Brotli
import org.http4k.core.ContentEncoding
import org.http4k.core.then
import org.http4k.fanbelt.events.ReportFailures
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.filter.PolyFilters
import org.http4k.routing.poly
import org.http4k.template.HandlebarsTemplates

fun App(
    runtime: AppRuntime = AppRuntime(),
    infrastructure: (AppRuntime) -> AppInfra = ::AppInfra,
    ciToken: String = "local-ci",
    statusService: Uri = Uri.of("http://localhost:9090"),
): Node {
    val infra = infrastructure(runtime)
    val spaces = Spaces(infra.storage)
    val grid = GridFeature()
    val builds = BuildsFeature(runtime.random)
    val notes = NotesFeature(runtime.random)
    val announcements = AnnouncementsFeature(notes, runtime.random)
    val rooms = RoomsFeature(announcements, runtime.random)
    val status = StatusFeature(ClientFilters.SetBaseUriFrom(statusService).then(runtime.http), runtime.clock)
    val workspace = Workspace(infra, "/spaces/{space}", spaces::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()

    val oneSpace = poly(
        listOf(GridRoutes(workspace, grid), NotesRoutes(workspace, notes), RoomRoutes(workspace, rooms)),
        "/" bind OverviewPage(workspace, renderer),
        "/plan" bind GridPage(workspace, grid, renderer),
        "/builds" bind BuildsPage(workspace, builds, renderer),
        "/status" bind StatusPage(workspace, status, renderer),
        "/notes" bind NotesPage(workspace, notes, renderer),
        "/rooms" bind RoomsPage(workspace, rooms, renderer),
        "/rooms/{room}" bind RoomPage(workspace, rooms, renderer),
    )

    val machines = workspace.routes(poly(BuildsWebhook(workspace, builds, ciToken)))

    val home = poly(EnsureViewer(runtime.random).then(HomeRoutes(spaces, workspace, renderer, runtime.clock)))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, machines, workspace.routes(identified.then(oneSpace))))

    return Node(app, runtime, infra, Jobs(infra, announcements.opened))
}
