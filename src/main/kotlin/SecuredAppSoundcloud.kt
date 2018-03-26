
import org.http4k.client.ApacheClient
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.OAuth
import org.http4k.security.soundCloud
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.renderToResponse
import java.time.Clock

fun main(args: Array<String>) {

    val home = Uri.of(("http://localhost:9000"))

    val clock = Clock.systemUTC()

    val oauth = OAuth.soundCloud(
        DebuggingFilters.PrintRequestAndResponse().then(ApacheClient()),
        Credentials(System.getenv("CLIENT_ID"), System.getenv("CLIENT_SECRET")),
        home, clock = clock
    )

    val templates = HandlebarsTemplates().CachingClasspath()
    val app: HttpHandler =
        routes(
            routes("/" bind GET to oauth.callback),
            oauth.authFilter.then(
                routes("/start" bind GET to { templates.renderToResponse(Index("app")) })
            )
        )

    ServerFilters.CatchAll()
        .then(DebuggingFilters.PrintRequestAndResponse())
        .then(app)
        .asServer(SunHttp(9000)).start().block()
}
