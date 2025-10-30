package org.http4k.ai.llm.chat

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ToolName
import org.http4k.connect.anthropic.AnthropicModels
import org.http4k.connect.anthropic.FakeAnthropicAI
import org.http4k.connect.anthropic.MessageContentGenerator
import org.http4k.connect.anthropic.ToolUseId
import org.http4k.connect.anthropic.action.Content
import org.http4k.util.PortBasedTest

class FakeAnthropicTest : ChatContract, PortBasedTest {

    private val http =
        FakeAnthropicAI(completionGenerators = mapOf(AnthropicModels.Claude_Haiku_4_5 to MessageContentGenerator { it ->
            when {
                it.last().toString().contains("ToolResult") -> listOf(Content.Text("four"))
                it.last().toString().contains("use the tool") -> listOf(
                    Content.ToolUse(
                        ToolName.of("calculator"),
                        ToolUseId.of("1"),
                        mapOf("first-arg" to 2.0, "second-arg" to 2.0)
                    )
                )

                it.last().toString().contains("what is 2+2? do not explain") -> listOf(Content.Text("4"))
                else -> listOf()
            }
        }))

    override val chat = Chat.AnthropicAI(ApiKey.of("asd"), http)

    override val model = AnthropicModels.Claude_Haiku_4_5
}
