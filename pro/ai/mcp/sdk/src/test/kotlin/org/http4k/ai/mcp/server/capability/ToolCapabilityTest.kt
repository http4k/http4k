package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson.asFormatString
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.security.ResponseType
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(JsonApprovalTest::class)
class ToolCapabilityTest {

    @Test
    fun `can convert to json schema`(approver: Approver) {
        approver.assertApproved(
            asFormatString(
                Tool(
                    "tool", "description",
                    Tool.Arg.string().optional("foo", "bar"),
                    Tool.Arg.int().required("bar", "foo"),
                    Tool.Arg.enum<ResponseType>().required("enum", "foo"),
                    Tool.Arg.string().multi.required("multibar", "foo")
                ).toSchema()
            ), APPLICATION_JSON
        )
    }

    @Test
    fun `tool returning Task creates task response`() {
        val taskId = TaskId.of("test-task-123")
        val now = Instant.parse("2024-01-15T10:30:00Z")
        val task = Task(taskId, TaskStatus.working, "Processing...", now, now)

        val tool = Tool("async-tool", "An async tool")
        val capability = ToolCapability(tool) {
            ToolResponse.Task(task)
        }

        val response = capability.call(
            McpTool.Call.Request(tool.name),
            NoOp,
            Request(GET, "/")
        )

        assertThat(response, equalTo(McpTool.Call.Response(task = task)))
    }
}
