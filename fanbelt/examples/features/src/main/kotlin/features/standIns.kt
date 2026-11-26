package features

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

private val moods = listOf("All systems go", "Degraded: builds are slow", "All systems go", "Investigating: some logins are failing")

fun StandInStatusService(clock: Clock = Clock.systemUTC()): Http4kServer =
    { _: Request -> Response(OK).body(moods[(clock.instant().epochSecond / 60 % moods.size).toInt()]) }.asServer(Jetty(0))

fun StandInCi(space: Uri, token: String = "local-ci", http: HttpHandler = JavaHttpClient()): AutoCloseable {
    val ticks = Executors.newSingleThreadScheduledExecutor { Thread(it, "stand-in-ci").apply { isDaemon = true } }
    ticks.scheduleAtFixedRate({
        val status = if (Random.nextInt(4) == 0) "Failing" else "Passing"
        val commit = Random.nextLong().toULong().toString(16).padStart(16, '0').take(7)
        runCatching {
            http(Request(POST, space.extend(Uri.of("builds"))).header("authorization", "Bearer $token")
                .header("content-type", "application/json").body("""{"status":"$status","commit":"$commit"}"""))
        }
    }, 2, 20, SECONDS)
    return AutoCloseable { ticks.shutdownNow() }
}
