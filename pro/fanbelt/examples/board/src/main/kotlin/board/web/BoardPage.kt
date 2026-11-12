package board.web

import board.domain.Column
import board.domain.Role
import board.domain.Viewer
import board.feature.BoardFeature
import board.feature.BuildFeature
import board.view.BoardPageView
import board.view.BuildRow
import board.view.BuildsView
import board.view.CardSignals
import board.view.CardView
import board.view.CardsView
import board.view.ColumnView
import board.view.ViewerView
import board.view.ViewersView
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun BoardPage(workspace: Workspace, boards: BoardFeature, ci: BuildFeature, renderer: TemplateRenderer): Page {
    val cards = Region("cards", renderer, boardId, boards.board) { model, request ->
        val board = boardId(request)
        val editor = model.roles[principal(request).id] == Role.Editor
        CardsView(board, editor, Column.entries.map { column ->
            ColumnView(column.name, model.cards.filter { it.column == column }.map {
                CardView(it.id, it.text, column != Column.entries.first(), column != Column.entries.last())
            })
        })
    }

    val viewers = Region("viewers", renderer, boardId, boards.viewers) { model, request ->
        val me = principal(request).id
        val iEdit = model.roles[me] == Role.Editor
        ViewersView(boardId(request), model.here.map { id ->
            val theirs = model.roles[id] ?: Role.Viewer
            ViewerView(
                id, Viewer(id).name, theirs, id == me,
                canPromote = iEdit && id != me && theirs == Role.Viewer,
                canDemote = iEdit && id != me && theirs == Role.Editor,
            )
        }.sortedBy { it.name })
    }

    val builds = Region("builds", renderer, boardId, ci.builds) { history, _ ->
        BuildsView(history.firstOrNull()?.status?.name ?: "No builds yet", history.map { BuildRow(it.status.name, it.commit.value) })
    }

    return Page(
        workspace = workspace,
        layout = Layout(
            regions = listOf(viewers, builds, cards),
            renderer = renderer,
            signals = CardSignals(),
        ) { request, shell -> BoardPageView(shell, boardId(request), principal(request).name) },
        viewing = Viewing { scope, request -> boards.here.join(scope, boardId(request), principal(request).id) },
    )
}