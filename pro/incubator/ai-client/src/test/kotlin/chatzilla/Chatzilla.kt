package chatzilla

import chatzilla.Settings.ANTHROPIC_API_KEY
import chatzilla.Settings.MCP_URL
import chatzilla.Settings.MODEL
import chatzilla.endpoints.ApproveTool
import chatzilla.endpoints.DenyTool
import chatzilla.endpoints.GetHistory
import chatzilla.endpoints.GetMessageForm
import chatzilla.endpoints.Index
import chatzilla.endpoints.SendUserMessage
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.AnthropicAI
import org.http4k.ai.llm.chat.Chat
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionStateMachine
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.McpLLMTools
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.filter.debug
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.DatastarFragmentRenderer
import org.http4k.template.HandlebarsTemplates

fun Chatzilla(env: Environment): PolyHandler {
    val http = JavaHttpClient().debug()

    val renderer = HandlebarsTemplates().CachingClasspath()
    val datastarRenderer = DatastarFragmentRenderer(renderer)

    val history = ChatHistory.InMemory()

    val client = HttpStreamingMcpClient(McpEntity.of("mcp"), Version.of("1.0"), MCP_URL(env), http)

    val stateMachine = ChatSessionStateMachine(
        Chat.AnthropicAI(ANTHROPIC_API_KEY(env), http),
        LLMMemory.InMemory(),
        McpLLMTools(client)
    ) { ModelParams(MODEL(env), tools = it) }
        .apply { start().valueOrNull()!! }

    val handler = ChatSessionHandler(stateMachine)

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
