package org.http4k.ai.llm.chat

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.model.ResponseFormat.Text
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.util.LLMJson
import org.http4k.ai.llm.util.LLMJson.convert
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Temperature.Companion.ZERO
import org.http4k.ai.model.ToolName
import org.junit.jupiter.api.Test

interface ChatContract {

    val chat: Chat
    val model: ModelName

    @Test
    fun `can ask a simple question text`() {
        val response = chat(
            ChatRequest(
                "what is 2+2? do not explain, do not use tools, just give the answer",
                ModelParams(model, ZERO, responseFormat = Text)
            )
        ).valueOrNull()!!

        System.err.println(response)
        assertThat(response.message.contents, equalTo(listOf(Content.Text("4"))))
        assertThat(response.message.toolRequests, equalTo(listOf()))
        assertThat(response.metadata.usage?.total, present(greaterThanOrEqualTo(0)))
    }

    val jsonSchema
        get() = LLMJson.parse(
            """
{
  "type": "object",
  "properties": {
        "first-arg": {
          "type": "number"
        },
        "second-arg": {
          "type": "number"
        }
    },
    "required": [
        "first-arg",
        "second-arg"
    ]
} """.trimIndent()
        )

    @Test
    fun `can generate a tool request`() {
        val response = chat(
            ChatRequest(
                "what is 2+2? do not explain. use the tool to get the answer",
                ModelParams(
                    model, ZERO,
                    tools = listOf(LLMTool("calculator", "A simple calculator", convert(jsonSchema))),
                )
            )
        ).valueOrNull()!!

        assertThat(response.message.toolRequests.size, equalTo(1))
        assertThat(response.message.toolRequests[0].name, equalTo(ToolName.of("calculator")))
        assertThat(response.message.toolRequests[0].arguments, equalTo(mapOf("first-arg" to 2.0, "second-arg" to 2.0)))
        assertThat(response.metadata.usage?.total, present(greaterThanOrEqualTo(0)))
    }
}
