package chatzilla

import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.chat.Chat
import org.http4k.ai.llm.chat.ChatRequest
import org.http4k.ai.llm.chat.ChatResponse
import org.http4k.ai.llm.chat.ChatResponse.Metadata
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.Message.Assistant
import org.http4k.ai.llm.model.Message.ToolResult
import org.http4k.ai.llm.model.Message.User
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ResponseId

class AChatLLM : Chat {
    override fun invoke(request: ChatRequest): LLMResult<ChatResponse> {
        val lastMessage = request.messages.last()

        println("processing $lastMessage")
        val response = when (lastMessage) {
            is User -> Success(
                ChatResponse(
                    Assistant(
                        emptyList(), listOf(
                            ToolRequest(RequestId.of("tool-1"), request.params.tools[0].name, mapOf("name" to "Bob"))
                        )
                    ),
                    Metadata(ResponseId.of("1"), ModelName.of("foo"))
                )
            )

            is ToolResult -> Success(
                when (lastMessage.tool) {
                    getFullNameTool.name ->
                        ChatResponse(
                            Assistant(
                                emptyList(), listOf(
                                    ToolRequest(
                                        RequestId.of("tool-2"),
                                        request.params.tools[1].name,
                                        mapOf("name" to lastMessage.text)
                                    )
                                )
                            ),
                            Metadata(ResponseId.of("2"), ModelName.of("foo"))
                        )

                    greetingTool.name ->
                        ChatResponse(
                            Assistant(listOf(Content.Text("Hello ${lastMessage.text}!")), listOf()),
                            Metadata(ResponseId.of("3"), ModelName.of("foo"))
                        )

                    else -> error("Unexpected message type ${lastMessage.tool}")
                }
            )

            else -> error("Unexpected message type $lastMessage")
        }
        println(response)
        return response
    }
}
