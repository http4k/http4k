package board.feature

import board.domain.BoardId
import board.domain.BoardModel
import board.domain.CardId
import board.domain.CardMove
import board.domain.Role
import board.domain.RoleChange
import board.domain.ViewerId
import board.domain.ViewersModel
import board.domain.cardText
import board.store.BoardTables
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.orElse
import java.util.Random

class BoardFeature(random: Random) : Feature<Boards, ViewerId>({ tables -> Boards(BoardTables(tables), random) }) {
    val board by state(BoardId) { boards, id -> BoardModel(boards.cards(id), boards.roles(id)) }

    val roles by state(BoardId) { boards, id -> boards.roles(id) }

    val here by presence(BoardId, ViewerId)

    val viewers by derived { ctx, id: BoardId -> ViewersModel(ctx.read(here, id).keys, ctx.read(roles, id)) }

    val join by operation { boards, by, id: BoardId ->
        if (boards.join(id, by)) Changes.of(board.topic(id), roles.topic(id)) else Changes.none()
    }

    val editableBoard by rule { ctx, by, id: BoardId -> if (ctx.domain.edits(id, by)) Allowed(id) else Denied }

    val allowedRoleChange by rule { ctx, by, change: RoleChange ->
        if (ctx.domain.edits(change.board, by) && change.member != by) Allowed(change) else Denied
    }

    val add by operation(editableBoard, Pair<BoardId, String>::first) { boards, _, (_, raw), id ->
        val text = cardText(raw).orElse { return@operation it }
        boards.addCard(id, text)
        Changes.of(board.topic(id))
    }

    val move by operation(editableBoard, CardMove::board) { boards, _, move, _ ->
        boards.move(move)
        Changes.of(board.topic(move.board))
    }

    val delete by operation(editableBoard, Pair<BoardId, CardId>::first) { boards, _, (id, card), _ ->
        boards.deleteCard(id, card)
        Changes.of(board.topic(id))
    }

    val setRole by operation(allowedRoleChange) { boards, _, change, _ ->
        boards.setRole(change)
        Changes.of(board.topic(change.board), roles.topic(change.board))
    }

    private fun Boards.edits(board: BoardId, viewer: ViewerId) = roleOf(board, viewer) == Role.Editor
}