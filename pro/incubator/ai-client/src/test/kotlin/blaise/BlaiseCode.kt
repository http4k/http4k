package blaise

import blaise.Settings.ANTHROPIC_API_KEY
import blaise.Settings.MCP_URL
import blaise.Settings.MODEL
import blaise.endpoints.ApproveTool
import blaise.endpoints.DenyTool
import blaise.endpoints.GetHistory
import blaise.endpoints.GetMessageForm
import blaise.endpoints.Index
import blaise.endpoints.SendUserMessage
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.chat.AnthropicAI
import org.http4k.ai.llm.chat.Chat
import org.http4k.ai.llm.chat.SessionHandler
import org.http4k.ai.llm.chat.SessionStateMachine
import org.http4k.ai.llm.chat.debug
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.McpLLMTools
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.core.BodyMode.Stream
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.HandlebarsTemplates

fun BlaiseCode(env: Environment): PolyHandler {
    val http = JavaHttpClient(responseBodyMode = Stream)

    val renderer = HandlebarsTemplates().CachingClasspath()
    val datastarRenderer = DatastarElementRenderer(renderer)

    val history = History.InMemory()

    val client = HttpStreamingMcpClient(McpEntity.of("mcp"), Version.of("1.0"), MCP_URL(env), http)

    val llm = Chat.AnthropicAI(ANTHROPIC_API_KEY(env), http)

    val stateMachine = SessionStateMachine(
        llm.debug(),
        LLMMemory.InMemory(),
        McpLLMTools(client),
//        setOf(DeleteFile.name, EditFile.name, ListFiles.name, ReadFile.name)
    ) { ModelParams(MODEL(env), tools = it) }
        .apply { start().valueOrNull()!! }

    val handler = SessionHandler(stateMachine)

    return poly(
        SendUserMessage(history, datastarRenderer, handler),
        ApproveTool(history, datastarRenderer, handler),
        DenyTool(history, datastarRenderer, handler),
        ServerFilters.CatchAll().then(
            routes(
                GetMessageForm(datastarRenderer),
                GetHistory(history, renderer),
                Index(renderer),
                static(Classpath("public")),
            )
        )
    )
}
