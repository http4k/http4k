package board

import board.domain.BoardId
import board.domain.BuildPosted
import board.domain.CardMove
import board.domain.CardText
import board.domain.Ci
import board.domain.Column
import board.domain.Direction
import board.domain.Role
import board.domain.RoleChange
import board.domain.Viewer
import board.domain.ViewerId
import board.feature.BoardFeature
import board.feature.BuildFeature
import org.http4k.fanbelt.testing.TestTenant

class DomainBoardDriver : BoardDriver {
    private val tenant = TestTenant()
    private val feature = BoardFeature(tenant.random)
    private val ci = BuildFeature(tenant.random)

    override fun member(name: String): BoardMember = DomainMember(viewerNamed(name))

    override fun ciReports(board: BoardId, status: String, commit: String) = tenant { ci.report.execute(Ci, board to BuildPosted(status, commit)) }

    private inner class DomainMember(override val id: ViewerId) : BoardMember {
        override val name = Viewer(id).name

        override fun opens(board: BoardId) {
            tenant { feature.join.execute(id, board) }
        }

        override fun watches(board: BoardId) {
            tenant { feature.here.join(board, id) }
            seen(board)
        }

        override fun adds(board: BoardId, text: String) = tenant { feature.add.execute(id, board to text) }

        override fun moves(board: BoardId, text: CardText, direction: Direction) {
            tenant { feature.move.execute(id, CardMove(board, cardOf(board, text), direction)) }
        }

        override fun deletes(board: BoardId, text: CardText) {
            tenant { feature.delete.execute(id, board to cardOf(board, text)) }
        }

        override fun makesEditor(board: BoardId, other: BoardMember) {
            tenant { feature.setRole.execute(id, RoleChange(board, other.id, Role.Editor)) }
        }

        override fun sees(board: BoardId) = seen(board)

        private fun cardOf(board: BoardId, text: CardText) = tenant { feature.board.read(board).cards.first { it.text == text }.id }

        private fun seen(board: BoardId): BoardSeen {
            val model = tenant { feature.board.read(board) }
            val viewers = tenant { feature.viewers.read(board) }
            return BoardSeen(
                Column.entries.associate { column -> column to model.cards.filter { it.column == column }.map { it.text } },
                model.roles[id] == Role.Editor,
                viewers.here.associate { Viewer(it).name to (viewers.roles[it] ?: Role.Viewer) },
                tenant { ci.builds.read(board).map { BuildSeen(it.status, it.commit) } },
            )
        }
    }
}