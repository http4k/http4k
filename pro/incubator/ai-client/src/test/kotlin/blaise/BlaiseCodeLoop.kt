package blaise

import blaise.Settings.ANTHROPIC_API_KEY
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import dev.forkhandles.result4k.valueOrNull
import mcp.mcpServer
import org.http4k.ai.llm.chat.AnthropicAI
import org.http4k.ai.llm.chat.Chat
import org.http4k.ai.llm.chat.SessionEvent.UserInput
import org.http4k.ai.llm.chat.SessionState.Responding
import org.http4k.ai.llm.chat.SessionStateMachine
import org.http4k.ai.llm.chat.debug
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.Content.Text
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.McpLLMTools
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.anthropic.AnthropicModels.Claude_Sonnet_4_5
import org.http4k.core.Uri


fun main() {
    val mcp = mcpServer(8000).start()

    val client =
        HttpStreamingMcpClient(McpEntity.of("mcp"), Version.of("1.0"), Uri.of("http://localhost:${mcp.port()}/mcp"))

    val tools = DebuggingLLMTools(McpLLMTools(client))

    val stateMachine = SessionStateMachine(
        Chat.AnthropicAI(ANTHROPIC_API_KEY(ENV)),
        LLMMemory.InMemory(),
        tools,
        tools.list().map { it.map { it.name } }.valueOrNull()!!.toSet()
    ) { ModelParams(Claude_Sonnet_4_5, tools = it) }
        .apply { start().valueOrNull()!! }

    loop@ while (true) {
        print("> ")
        println(
            stateMachine(UserInput(readlnOrNull() ?: ""))
                .map { if (it is Responding) it.asString() }
        )
    }
}

private fun Responding.asString() = contents.filterIsInstance<Text>().joinToString("\n") { it.text }
