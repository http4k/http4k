package board

import board.domain.BoardId
import board.domain.BuildPosted
import board.store.BoardJson
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.fanbelt.testing.Verdict

fun HttpHandler.reportsBuild(board: BoardId, status: String, commit: String, token: String = "local-ci"): Verdict {
    val response = ClientFilters.BearerAuth(token).then(this)(with(BoardJson) { Request(POST, "/teams/acme/b/${board.value}/builds").json(BuildPosted(status, commit)) })
    return when (response.status) {
        ACCEPTED -> Verdict.Accepted
        BAD_REQUEST -> Verdict.Rejected(response.bodyString().lines().toSet())
        FORBIDDEN -> Verdict.Denied
        else -> error("CI was answered ${response.status}")
    }
}