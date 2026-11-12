package board

import board.domain.BoardId
import board.domain.BuildStatus
import board.domain.CardText
import board.domain.Column
import board.domain.Commit
import board.domain.Direction
import board.domain.Role
import board.domain.ViewerId
import org.http4k.fanbelt.testing.Verdict

interface BoardDriver {
    fun member(name: String): BoardMember
    fun ciReports(board: BoardId, status: String, commit: String): Verdict
}

interface BoardMember {
    val id: ViewerId
    val name: String
    fun opens(board: BoardId)
    fun watches(board: BoardId)
    fun adds(board: BoardId, text: String): Verdict
    fun moves(board: BoardId, text: CardText, direction: Direction)
    fun deletes(board: BoardId, text: CardText)
    fun makesEditor(board: BoardId, other: BoardMember)
    fun sees(board: BoardId): BoardSeen
}

data class BoardSeen(val columns: Map<Column, List<CardText>>, val canEdit: Boolean, val members: Map<String, Role>, val builds: List<BuildSeen>)

data class BuildSeen(val status: BuildStatus, val commit: Commit)