package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.InMemory
import org.http4k.ai.llm.tools.LLMTools
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ResponseId
import org.junit.jupiter.api.Test

class SessionStateMachineTest {
    private val machine = SessionStateMachine(
        StaticChat,
        LLMMemory.InMemory(),
        LLMTools.InMemory(emptyMap()),
        emptySet(),
        { ModelParams(ModelName.of("test")) }
    )

    @Test
    fun `passing through states`() {
        val memoryId = machine.start().valueOrNull()

        machine(SessionEvent.UserInput("test"))

    }
}

object StaticChat : Chat {
    override fun invoke(request: ChatRequest): LLMResult<ChatResponse> =
        Success(
            ChatResponse(
                Message.Assistant(emptyList(), emptyList()),
                ChatResponse.Metadata(ResponseId.of(""), request.params.modelName)
            )
        )
}
