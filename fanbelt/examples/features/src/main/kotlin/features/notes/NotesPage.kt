package features.notes

import features.Viewer
import features.principal
import org.http4k.core.Method.POST
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

private const val PAGE = 20

val show = Query.value(Shown).defaulted("show", Shown.of(PAGE))

data class NoteSignals(val note: String = "")

data class NoteRow(val by: String, val text: String)

data class NotesView(val notes: List<NoteRow>, val more: Int?) : ViewModel

data class NotesPageView(val shell: PageShell) : ViewModel

fun NotesPage(workspace: Workspace, notes: NotesFeature, renderer: TemplateRenderer): Page {
    val newest = Region("notes", renderer, { show(it) }, notes.newest) { window, shown ->
        NotesView(window.notes.map { NoteRow(it.by?.let { by -> Viewer(by).name } ?: "Rooms", it.text.value) }, if (window.more) shown.value + PAGE else null)
    }
    return Page(workspace, Layout(listOf(newest), renderer, signals = NoteSignals()) { shell -> NotesPageView(shell) })
}

fun NotesRoutes(workspace: Workspace, notes: NotesFeature) = routes(
    "/notes" bind POST to workspace.execute(notes.post, { principal(it).id }, reset = setOf(NoteSignals::note)) { signals: NoteSignals -> signals.note },
)
