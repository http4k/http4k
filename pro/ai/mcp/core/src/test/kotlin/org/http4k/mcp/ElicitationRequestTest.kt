package org.http4k.mcp

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.mcp.model.Elicitation
import org.http4k.mcp.model.Elicitation.Metadata.integer.Max
import org.http4k.mcp.model.Elicitation.Metadata.integer.Min
import org.http4k.mcp.model.Elicitation.Metadata.string.Format.Date
import org.http4k.mcp.model.Elicitation.Metadata.string.MaxLength
import org.http4k.mcp.model.Elicitation.Metadata.string.MinLength
import org.http4k.mcp.model.Elicitation.Metadata.string.Pattern
import org.http4k.mcp.model.boolean
import org.http4k.mcp.model.int
import org.http4k.mcp.model.number
import org.http4k.mcp.model.string
import org.http4k.mcp.util.McpJson.pretty
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ElicitationRequestTest {

    @Test
    fun `can create schema from outputs`(approver: Approver) {

        val request = ElicitationRequest(
            "foo",
            Elicitation.string().required(
                "reqStr", "title", "description",
                Date,
                MinLength(1),
                MaxLength(10),
                Pattern(Regex(".*"))
            ),
            Elicitation.int().optional(
                "optInt", "title", "description",
                Min(1),
                Max(100)
            ),
            Elicitation.boolean().defaulted(
                "defBool",
                true,  "title", "description",
                Elicitation.Metadata.boolean.Default(true)
            ),
            Elicitation.number().optional("optNum", "title", "description")
        )
        approver.assertApproved(pretty(request.requestedSchema), APPLICATION_JSON)
    }
}
