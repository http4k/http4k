package board.feature

import board.domain.BoardId
import board.domain.Card
import board.domain.CardId
import board.domain.CardMove
import board.domain.CardText
import board.domain.Column
import board.domain.Column.Todo
import board.domain.Direction
import board.domain.Direction.Right
import board.domain.Member
import board.domain.MemberId
import board.domain.Role
import board.domain.RoleChange
import board.domain.ViewerId
import board.store.BoardTables
import dev.forkhandles.values.random
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.first
import java.util.Random

class Boards(private val tables: BoardTables, private val random: Random) {
    fun cards(board: BoardId) = tables.cards.page(where = Card::board eq board).items

    fun addCard(board: BoardId, text: CardText) = tables.cards.insert(Card(CardId.random(random), board, Todo, text))

    fun move(move: CardMove) =
        tables.cards.update(move.card) { card -> if (card.board == move.board) card.copy(column = card.column.step(move.direction)) else card }

    fun deleteCard(board: BoardId, id: CardId) {
        if (tables.cards.get(id)?.board == board) tables.cards.delete(id)
    }

    fun members(board: BoardId) = tables.members.page(where = Member::board eq board).items

    fun roles(board: BoardId) = members(board).associate { it.viewer to it.role }

    fun roleOf(board: BoardId, viewer: ViewerId) = tables.members.get(memberId(board, viewer))?.role

    fun join(board: BoardId, viewer: ViewerId): Boolean {
        if (roleOf(board, viewer) != null) return false
        val role = if (tables.members.first(where = Member::board eq board) == null) Role.Editor else Role.Viewer
        tables.members.insert(Member(memberId(board, viewer), board, viewer, role))
        return true
    }

    fun setRole(change: RoleChange) = tables.members.update(memberId(change.board, change.member)) { it.copy(role = change.role) }

    private fun Column.step(direction: Direction): Column {
        val all = Column.entries
        return all[(ordinal + if (direction == Right) 1 else -1).coerceIn(0, all.lastIndex)]
    }

    private fun memberId(board: BoardId, viewer: ViewerId) = MemberId.of("$board:$viewer")
}