package org.http4k.ai.llm.chat

import org.http4k.ai.llm.image.ImageGeneration
import org.http4k.ai.llm.image.ImageGenerationContract
import org.http4k.ai.llm.image.OpenAI
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.Role
import org.http4k.connect.openai.ChatCompletionGenerator
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.OpenAIModels
import org.http4k.connect.openai.action.Choice
import org.http4k.connect.openai.action.ChoiceDetail
import org.http4k.connect.openai.action.FunctionCall
import org.http4k.connect.openai.action.ToolCall
import org.http4k.util.PortBasedTest

class FakeOpenAIChatTest : ChatContract, StreamingChatContract, ImageGenerationContract, PortBasedTest {

    private val http = FakeOpenAI(completionGenerators = mapOf(OpenAIModels.GPT3_5 to ChatCompletionGenerator { it ->
        when {
            it.messages.last().toString().contains("role=tool") -> listOf(
                Choice(
                    0,
                    ChoiceDetail(Role.Assistant, "four"),
                    null,
                    null
                )
            )

            it.messages.last().toString().contains("use the tool to get the answer") -> listOf(
                Choice(
                    0, ChoiceDetail(
                        Role.Assistant, null, listOf(
                            ToolCall(
                                "1", "function", FunctionCall(
                                    "calculator",
                                    """{"first-arg":2.0, "second-arg":2.0}"""
                                )
                            )
                        )
                    ), null, null
                )
            )

            it.messages.last().toString().contains("what is 2+2? do not explain") -> listOf(
                Choice(
                    0,
                    ChoiceDetail(Role.Assistant, "4"),
                    null,
                    null
                )
            )

            else -> listOf()
        }
    }))

    override val chat = Chat.OpenAI(ApiKey.of("asd"), http)
    override val streamingChat = StreamingChat.OpenAI(ApiKey.of("asd"), http)
    override val imageGeneration = ImageGeneration.OpenAI(ApiKey.of("asd"), http)

    override val model = OpenAIModels.GPT3_5
}
