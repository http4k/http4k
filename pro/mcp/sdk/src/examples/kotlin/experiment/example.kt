package experiment

import org.http4k.filter.debug
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    mcpSse(
        ServerMetaData("mcp", "1.0.0"),
        Reference.of(PromptName.of("Dinner")) bind { it: CompletionRequest ->
            CompletionResponse(listOf("David", "Al", "Franck"))
        }
    ).debug().asServer(Helidon(7501)).start()
}
