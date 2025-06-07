package chatzilla

import chatzilla.endpoints.ApproveTool
import chatzilla.endpoints.DenyTool
import chatzilla.endpoints.GetHistory
import chatzilla.endpoints.GetMessageForm
import chatzilla.endpoints.Index
import chatzilla.endpoints.SendUserMessage
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionStateMachine
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.McpLLMTools
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.filter.debug
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.DatastarFragmentRenderer
import org.http4k.template.HandlebarsTemplates

fun Chatzilla2(mcp: Uri): HttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val datastarRenderer = DatastarFragmentRenderer(renderer)

    val history = ChatHistory("Welcome to Chatzilla!")

    val client = HttpStreamingMcpClient(McpEntity.of("mcp"), Version.of("1.0"), mcp)
    val stateMachine = ChatSessionStateMachine(
        AChatLLM(),
        LLMMemory.InMemory(),
        McpLLMTools(client)
    ) { ModelParams(ModelName.of("hello"), tools = it) }.start()

    val handler = ChatSessionHandler(stateMachine)

    return ServerFilters.CatchAll().then(
        routes(
            GetMessageForm(datastarRenderer),
            SendUserMessage(history, datastarRenderer, handler),
            ApproveTool(history, datastarRenderer),
            DenyTool(history, datastarRenderer),
            GetHistory(history, renderer),
            Index(renderer),
            static(Classpath("public")),
        )
    ).debug()
}

