package board.feature

import board.domain.BoardId
import board.domain.BuildPosted
import board.domain.BuildReport
import board.domain.BuildStatus
import board.domain.Ci
import board.domain.Commit
import board.store.BoardTables
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.feature.zip
import org.http4k.fanbelt.input.FieldName
import java.util.Random

class BuildFeature(random: Random) : Feature<BuildLog, Ci>({ tables -> BuildLog(BoardTables(tables), random) }) {
    val builds by state(BoardId) { log, board -> log.latest(board) }

    val report by operation { log, _, (board, posted): Pair<BoardId, BuildPosted> ->
        val report = buildStatus(posted.status)
            .zip(FieldName.of("commit").check(Commit, posted.commit, "A commit is 7 to 40 hex characters")) { status, commit -> BuildReport(board, status, commit) }
            .orElse { return@operation it }
        log.record(report)
        Changes.of(builds.topic(report.board))
    }
}

private fun buildStatus(raw: String) = FieldName.of("status").check(BuildStatus.entries, raw, "A build is passing, failing or running")