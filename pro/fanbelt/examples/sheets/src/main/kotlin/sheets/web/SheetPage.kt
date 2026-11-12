package sheets.web

import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import sheets.domain.COLUMNS
import sheets.domain.CellRef
import sheets.domain.ROWS
import sheets.domain.RowNumber
import sheets.domain.Viewer
import sheets.domain.ViewerId
import sheets.feature.SheetFeature
import sheets.view.CellView
import sheets.view.CursorRule
import sheets.view.CursorsView
import sheets.view.EditSignals
import sheets.view.HereView
import sheets.view.RowView
import sheets.view.SheetPageView

fun SheetPage(workspace: Workspace, renderer: TemplateRenderer): Page {
    val rows = (1..ROWS).map { number ->
        Region("row$number", renderer, { RowNumber.of(number) }, SheetFeature.row) { cells, _ ->
            RowView(number, COLUMNS.map { column -> CellRef.of(column, number) }.map { ref -> CellView(ref, cells[ref]?.value.orEmpty()) })
        }
    }
    val here = Region("here", renderer, SheetFeature.here) { present, _ -> HereView(present.keys.map { Viewer(it).name }.sorted()) }
    val cursors = Region("cursors", renderer, SheetFeature.here) { present, _ ->
        val byCell = present.entries.mapNotNull { (who, cell) -> if (cell == null) null else cell to who }.groupBy({ it.first }, { it.second })
        CursorsView(byCell.flatMap { (cell, viewers) -> cursorRules(cell, viewers) })
    }
    return Page(
        workspace,
        Layout(rows + here + cursors, renderer, signals = EditSignals()) { request, shell ->
            SheetPageView(shell, principal(request).id, COLUMNS, (1..ROWS).map { shell.regions.getValue("row$it") })
        },
        Viewing { scope, request -> SheetFeature.arrive(scope, principal(request).id) },
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