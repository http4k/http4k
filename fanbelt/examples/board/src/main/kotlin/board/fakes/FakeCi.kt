package board.fakes

import board.domain.BuildPosted
import board.store.BoardJson
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FakeCi(private val url: String, token: String) : AutoCloseable {
    private val client = ClientFilters.BearerAuth(token).then(JavaHttpClient())
    private val loop = Executors.newSingleThreadScheduledExecutor()

    init {
        loop.scheduleWithFixedDelay({ runCatching { client(report(nextBuild())) } }, 2, 3, TimeUnit.SECONDS)
    }

    private fun report(build: BuildPosted) = with(BoardJson) { Request(POST, url).json(build) }

    private fun nextBuild() = BuildPosted(listOf("passing", "failing", "running").random(), (1..7).map { "0123456789abcdef".random() }.joinToString(""))

    override fun close() {
        loop.shutdownNow()
    }
}