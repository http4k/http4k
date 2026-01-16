package blaise

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.chat.AnthropicAI
import org.http4k.ai.llm.chat.Chat
import org.http4k.ai.llm.chat.SessionEvent
import org.http4k.ai.llm.chat.SessionState.Responding
import org.http4k.ai.llm.chat.SessionStateMachine
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.Content.Text
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.McpLLMTools
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.config.Environment
import org.http4k.connect.anthropic.AnthropicModels
import org.http4k.core.Uri

fun BlaiseCode(mcpUri: Uri, io: IO) {
    val client = HttpStreamingMcpClient(McpEntity.of("mcp"), Version.of("1.0"), mcpUri)

    val tools = DebuggingLLMTools(McpLLMTools(client), io::write)

    val stateMachine = SessionStateMachine(
        Chat.AnthropicAI(Settings.ANTHROPIC_API_KEY(Environment.ENV)),
        LLMMemory.InMemory(),
        tools,
        tools.list().map { it.map { it.name } }.valueOrNull()!!.toSet()
    ) { ModelParams(AnthropicModels.Claude_Sonnet_4_5, tools = it) }
        .apply { start().valueOrNull()!! }

    loop@ while (true) {
        io.write("> ")
        stateMachine(SessionEvent.UserInput(io.read()))
            .map { if (it is Responding) io.write(it.asString() + "\n") }
    }
}

private fun Responding.asString() = contents.filterIsInstance<Text>().joinToString("\n") { it.text }
