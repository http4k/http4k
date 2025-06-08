package org.http4k.ai.llm.memory

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.LLMError.NotFound
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import org.junit.jupiter.api.Test

interface LLMMemoryContract {

    val memory: LLMMemory

    @Test
    fun `lifecycle of memory`() {
        val user = Message.User(listOf(Content.Text("hello")))
        val assistant = Message.Assistant(
            listOf(Content.Text("assistant")),
            listOf(ToolRequest(RequestId.of("1"), ToolName.of("tool"), mapOf("arg" to "value")))
        )

        val system = Message.System("system")
        val tool = Message.ToolResult(RequestId.of("1"), ToolName.of("tool"), "text")
        val custom = Message.Custom(mapOf("key" to "value"))
        val memoryId = memory.create(listOf(user, assistant)).valueOrNull()!!

        assertThat(memory.read(memoryId), equalTo(Success(listOf(user, assistant))))

        assertThat(memory.update(memoryId, listOf(system, tool)), equalTo(Success(memoryId)))

        assertThat(memory.read(memoryId), equalTo(Success(listOf(system, tool))))

        assertThat(memory.add(memoryId, listOf(custom)), equalTo(Success(memoryId)))

        assertThat(memory.read(memoryId), equalTo(Success(listOf(system, tool, custom))))

        assertThat(memory.delete(memoryId), equalTo(Success(memoryId)))

        assertThat(memory.read(memoryId), equalTo(Failure(NotFound)))
    }
}

