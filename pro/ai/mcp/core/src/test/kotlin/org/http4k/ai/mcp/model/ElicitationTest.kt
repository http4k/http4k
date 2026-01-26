package org.http4k.ai.mcp.model

import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LocalDateValueFactory
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.model.Elicitation.Metadata.EnumMappings
import org.http4k.ai.mcp.model.Elicitation.Metadata.boolean.Default
import org.http4k.ai.mcp.model.Elicitation.Metadata.integer.Max
import org.http4k.ai.mcp.model.Elicitation.Metadata.integer.Min
import org.http4k.ai.mcp.model.Elicitation.Metadata.string.Format.Date
import org.http4k.ai.mcp.model.Elicitation.Metadata.string.MaxLength
import org.http4k.ai.mcp.model.Elicitation.Metadata.string.MinLength
import org.http4k.ai.mcp.model.Elicitation.Metadata.string.Pattern
import org.http4k.ai.mcp.model.ElicitationTest.FooBar.BAR
import org.http4k.ai.mcp.model.ElicitationTest.FooBar.FOO
import org.http4k.ai.mcp.model.EnumSelection.Single
import org.http4k.ai.mcp.util.McpJson.asFormatString
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(JsonApprovalTest::class)
class ElicitationTest {

    @Test
    fun `string to schema`(approver: Approver) {
        approver.approve(
            Elicitation.string().required(
                "name", "title", "description",
                Date,
                MinLength(1),
                MaxLength(10),
                Pattern(Regex(".*"))
            )
        )
    }

    @Test
    fun `int to schema`(approver: Approver) {
        approver.approve(
            Elicitation.int().required(
                "name", "title", "description",
                Min(1),
                Max(100),
            )
        )
    }

    class Foo private constructor(value: LocalDate) : LocalDateValue(value) {
        companion object : LocalDateValueFactory<Foo>(ElicitationTest::Foo)
    }

    @Test
    fun `value to schema`(approver: Approver) {
        approver.approve(Elicitation.value(Foo).required("name", "title", "description"))
    }

    @Test
    fun `boolean to schema`(approver: Approver) {
        approver.approve(
            Elicitation.boolean().defaulted(
                "name",
                true,
                "title", "description",
                Default(true),
            )
        )
    }

    enum class FooBar {
        FOO, BAR
    }

    @Test
    fun `enum to schema`(approver: Approver) {
        approver.approve(
            Elicitation.enum<FooBar>().required(
                "name", "title", "description",
                EnumMappings(Single(), mapOf(FOO to "Foo description", BAR to "Bar description"))
            )
        )
    }

    private fun Approver.approve(lens: McpCapabilityLens<ElicitationResponse, *>) {
        assertApproved(asFormatString(lens.toSchema()), APPLICATION_JSON)
    }
}
