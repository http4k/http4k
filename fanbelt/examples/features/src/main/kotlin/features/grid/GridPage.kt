package features.grid

import features.Viewer
import features.ViewerId
import features.principal
import org.http4k.fanbelt.feature.invoke
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun GridPage(workspace: Workspace, grid: GridFeature, renderer: TemplateRenderer): Page {
    val rows = (1..ROWS).map { number ->
        Region("row$number", renderer, { RowNumber.of(number) }, grid.row) { cells, _ ->
            RowView(number, COLUMNS.map { column -> CellRef.of(column, number) }.map { ref -> CellView(ref, cells[ref]?.value.orEmpty()) })
        }
    }
    val here = Region("here", renderer, grid.here) { present -> HereView(present.keys.map { Viewer(it).name }.sorted()) }
    val cursors = Region("cursors", renderer, grid.here) { present ->
        val byCell = present.entries.mapNotNull { (who, cell) -> if (cell == null) null else cell to who }.groupBy({ it.first }, { it.second })
        CursorsView(byCell.flatMap { (cell, viewers) -> cursorRules(cell, viewers) })
    }
    return Page(
        workspace,
        Layout(rows + here + cursors, renderer, signals = EditSignals()) { shell, (viewer) ->
            GridPageView(shell, viewer.id, COLUMNS, (1..ROWS).map { shell.regions.getValue("row$it") })
        },
        Viewing(principal, grid.arrive { it.actor.id }),
    )
}

private fun cursorRules(cell: CellRef, viewers: List<ViewerId>): List<CursorRule> {
    val elsewhere = CursorRule("body" + viewers.joinToString("") { ":not([data-viewer=\"$it\"])" }, cell, names(viewers))
    val here = viewers.mapNotNull { viewer ->
        val others = viewers - viewer
        if (others.isEmpty()) null else CursorRule("body[data-viewer=\"$viewer\"]", cell, names(others))
    }
    return listOf(elsewhere) + here
}

private fun names(viewers: List<ViewerId>) = viewers.map { Viewer(it).name }.sorted().joinToString(", ")