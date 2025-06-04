package org.http4k.ai.mcp.server.capability

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.util.McpJson.asFormatString
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
                    Tool.Arg.enum<ResponseType>().required("enum", "foo"),
                    Tool.Arg.string().multi.required("multibar", "foo")
                ).toSchema()
            ), APPLICATION_JSON
        )
    }
}
