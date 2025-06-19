package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMError.NotFound
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.llm.tools.ToolResponse
import org.http4k.ai.model.RequestId
import org.junit.jupiter.api.Test

class ToolRoutingTest {

    private val llmTool1 = LLMTool("Tool1", "Description of Tool1")
    private val llmTool2 = LLMTool("Tool2", "Description of Tool2")
    private val llmTool3 = LLMTool("Tool3", "Description of Tool3")
    private val llmTool4 = LLMTool("Tool4", "Description of Tool4")

    @Test
    fun `route to matching call`() {
        val tools = tools(
            tools(
                llmTool1 bind { Success(ToolResponse(it.id, llmTool1.name, "123")) },
                tools(llmTool2 bind { Success(ToolResponse(it.id, llmTool2.name, "234")) })
            ),
            llmTool3 bind { Success(ToolResponse(it.id, llmTool3.name, "345")) },
        )

        assertThat(
            tools(ToolRequest(RequestId.of("1"), llmTool1.name)), equalTo(
                Success(ToolResponse(RequestId.of("1"), llmTool1.name, "123"))
            )
        )

        assertThat(
            tools(ToolRequest(RequestId.of("2"), llmTool2.name)), equalTo(
                Success(ToolResponse(RequestId.of("2"), llmTool2.name, "234"))
            )
        )

        assertThat(
            tools(ToolRequest(RequestId.of("3"), llmTool3.name)), equalTo(
                Success(ToolResponse(RequestId.of("3"), llmTool3.name, "345"))
            )
        )

        assertThat(
            tools(ToolRequest(RequestId.of("4"), llmTool4.name)), equalTo(
                Failure(NotFound)
            )
        )
    }
}
