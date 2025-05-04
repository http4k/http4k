package org.http4k.mcp.server.capability

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.enum
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.model.Tool
import org.http4k.mcp.util.McpJson.asFormatString
import org.http4k.security.ResponseType
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
                    Tool.Arg.enum<ToolRequest, ResponseType>().required("enum", "foo"),
                    Tool.Arg.multi.required("multibar", "foo")
                ).toSchema()
            ), APPLICATION_JSON
        )
    }
}
