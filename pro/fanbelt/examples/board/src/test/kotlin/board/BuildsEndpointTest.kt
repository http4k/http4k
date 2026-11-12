package board

import board.domain.BuildPosted
import board.store.BoardJson
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildsEndpointTest {
    private val http = App(AppRuntime()).apply { start() }.handler.http!!

    init {
        http(Request(POST, "/teams").form("team", "acme"))
    }

    @Test
    fun `only CI, with its token, can report builds`() {
        val guessing = ClientFilters.BearerAuth("guessed").then(http)

        val response = guessing(with(BoardJson) { Request(POST, "/teams/acme/b/plans/builds").json(BuildPosted("passing", "abc1234")) })

        assertEquals(UNAUTHORIZED, response.status)
    }
}