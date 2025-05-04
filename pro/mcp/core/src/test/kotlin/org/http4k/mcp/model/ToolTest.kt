package org.http4k.mcp.model

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiLens
import org.http4k.lens.boolean
import org.http4k.lens.double
import org.http4k.lens.enum
import org.http4k.lens.int
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.util.McpJson
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ToolTest {

    enum class TestEnum {
        FOO, BAR
    }

    @Test
    fun `string to schema`(approver: Approver) {
        approver.assertApproved(Tool.Arg.optional("foo", "bar"))
    }

    @Test
    fun `integer to schema`(approver: Approver) {
        approver.assertApproved(Tool.Arg.int().optional("foo", "bar"))
    }

    @Test
    fun `boolean to schema`(approver: Approver) {
        approver.assertApproved(Tool.Arg.boolean().optional("foo", "bar"))
    }

    @Test
    fun `number to schema`(approver: Approver) {
        approver.assertApproved(Tool.Arg.double().optional("foo", "bar"))
    }

    @Test
    fun `array to schema`(approver: Approver) {
        approver.assertApproved(Tool.Arg.double().multi.optional("foo", "bar"))
    }

    @Test
    fun `enum to schema`(approver: Approver) {
        approver.assertApproved(Tool.Arg.enum<ToolRequest, TestEnum>().optional("foo", "bar"))
    }
}

private fun Approver.assertApproved(input: BiDiLens<ToolRequest, *>) {
    assertApproved(McpJson.asFormatString(input.toSchema()), APPLICATION_JSON)
}
