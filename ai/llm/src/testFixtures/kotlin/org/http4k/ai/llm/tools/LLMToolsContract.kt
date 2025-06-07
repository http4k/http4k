package org.http4k.ai.llm.tools

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMError
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.junit.jupiter.api.Test

interface LLMToolsContract {

    val llmTools: LLMTools
    val echoTool: LLMTool

    @Test
    fun `can list tools`() {
        assertThat(llmTools.list(), equalTo(Success(listOf(echoTool))))
    }

    @Test
    fun `can call a known tool`() {
        assertThat(
            llmTools(ToolRequest(RequestId.of("1"), echoTool.name, mapOf("arg" to "value"))),
            equalTo(
                Success(ToolResponse(RequestId.of(("1")), echoTool.name, text = "eulav"))
            )
        )
    }

    @Test
    fun `cannot call an unknown tool`() {
        assertThat(
            llmTools(ToolRequest(RequestId.of("1"), ToolName.of("foo"), mapOf("arg" to "value"))),
            equalTo(Failure(LLMError.Custom(InvalidParams)))
        )
    }
}
