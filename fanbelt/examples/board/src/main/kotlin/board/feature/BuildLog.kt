package board.feature

import board.domain.BoardId
import board.domain.Build
import board.domain.BuildId
import board.domain.BuildReport
import board.store.BoardTables
import dev.forkhandles.values.random
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.eq
import java.util.Random

class BuildLog(private val tables: BoardTables, private val random: Random) {
    fun latest(board: BoardId) = tables.builds.page(where = Build::board eq board, order = Newest, limit = 5).items

    fun record(report: BuildReport) = tables.builds.insert(Build(BuildId.random(random), report.board, report.status, report.commit))
}