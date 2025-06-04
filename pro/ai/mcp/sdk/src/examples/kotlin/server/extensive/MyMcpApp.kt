package server.extensive

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.EventFilters
import org.http4k.events.then
import org.http4k.format.Moshi
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.security.OAuthMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import java.time.Clock
import java.util.Random

fun MyMcpApp(
    storage: Storage,
    env: Environment = Environment.ENV,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(), random: Random = Random(0)
): PolyHandler {

    val events = EventFilters.AddEventName()
        .then(EventFilters.AddTimestamp())
        .then(EventFilters.AddZipkinTraces())
        .then(AutoMarshallingEvents(Moshi))

    val service = RemoteService.Http(Settings.BASE_URL(env), http, clock)
    val repo = storage.repo(env, http, random)

    val hub = Hub(repo, service)

    return mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        OAuthMcpSecurity(Uri.of("http://authserver")) { it == "123" },
        aTool(hub),
        aCompletion(hub),
    )
}
