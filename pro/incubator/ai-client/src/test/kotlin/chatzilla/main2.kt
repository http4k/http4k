package chatzilla

import org.http4k.ai.llm.util.LLMJson
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.Version
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.lens.with


fun main() {
    val mcp = mcpServer(8000).start()

    val client = HttpStreamingMcpClient(
        McpEntity.of("mcp"),
        Version.of("1.0"),
        Uri.of("http://localhost:${mcp.port()}/mcp"),
        JavaHttpClient(responseBodyMode = Stream).debug(),
        notificationSseReconnectionMode = Disconnect
    )
    client.start()

    Thread.sleep(1000)
    client.elicitations().onElicitation {
        ElicitationResponse(ElicitationAction.accept, LLMJson.obj("foo" to LLMJson.string("bar")), Meta(it.progressToken))
    }

    println(client.tools().call(getFullNameTool.name, ToolRequest(meta = Meta("123")).with(name of "John")))

    mcp.stop()
    client.stop()
}
