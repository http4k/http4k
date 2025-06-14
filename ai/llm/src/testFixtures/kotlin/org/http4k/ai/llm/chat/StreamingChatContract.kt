package org.http4k.ai.llm.chat

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.chat.ChatResponseFormat.Text
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Temperature.Companion.ZERO
import org.junit.jupiter.api.Test

interface StreamingChatContract {

    val streamingChat: StreamingChat
    val model: ModelName

    @Test
    fun `can ask a simple question text streaming`() {
        val stream = streamingChat(
            ChatRequest(
                "what is 2+2? do not explain, do not use tools, just give the answer. do not add whitespace to the answer",
                ModelParams(model, ZERO, responseFormat = Text)
            )
        ).valueOrNull()!!

        val message = stream
            .flatMap { it.message.contents.consolidate() }
            .toList()
        assertThat(message, equalTo(listOf(Content.Text("4"))))
    }
}
