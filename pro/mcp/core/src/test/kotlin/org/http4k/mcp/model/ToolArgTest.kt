package org.http4k.mcp.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.LensFailure
import org.http4k.lens.with
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpJson.auto
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(JsonApprovalTest::class)
class ToolArgTest {

    enum class TestEnum {
        FOO, BAR
    }

    @Test
    fun `string arg schema`(approver: Approver) {
        approver.check(Tool.Arg.string(), "test", "test")
    }

    @Test
    fun `boolean to schema`(approver: Approver) {
        approver.check(Tool.Arg.boolean(), true, true)
    }

    @Test
    fun `int to schema`(approver: Approver) {
        approver.check(Tool.Arg.int(), 123, 123)
    }

    @Test
    fun `double to schema`(approver: Approver) {
        approver.check(Tool.Arg.double(), 12.4, 12.4)
    }

    @Test
    fun `float to schema`(approver: Approver) {
        approver.check(Tool.Arg.float(), 12.4f, 12.4f)
    }

    @Test
    fun `uuid to schema`(approver: Approver) {
        approver.check(Tool.Arg.uuid(), UUID(0, 123), "00000000-0000-0000-0000-00000000007b")
    }

    @Test
    fun `long to schema`(approver: Approver) {
        approver.check(Tool.Arg.long(), Long.MAX_VALUE, Long.MAX_VALUE)
    }

    @Test
    fun `enum to schema`(approver: Approver) {
        approver.check(Tool.Arg.enum<TestEnum>(), TestEnum.BAR, TestEnum.BAR)
    }

    data class FooBar(val foo: String = "foo", val bar: Int = 0)

    @Test
    fun `auto to schema`(approver: Approver) {
        val spec = Tool.Arg.auto(FooBar()).required("foo")
        approver.assertApproved(McpJson.asFormatString(spec.toSchema()), APPLICATION_JSON)
    }

    private fun <T : Any> Approver.check(spec: ToolArgLensSpec<T>, value: T, mapValue: Any) {
        val optionalLens = spec.optional("foo", "bar")
        val optionalInjected = ToolRequest().with(optionalLens of value)
        assertThat(optionalInjected.args, equalTo(mapOf("foo" to mapValue)))
        assertThat(optionalLens(optionalInjected), equalTo(value))
        assertThat(optionalLens(ToolRequest()), equalTo(null))

        val requiredLens = spec.required("foo", "bar")
        val requiredInjected = ToolRequest().with(requiredLens of value)

        assertThat(requiredInjected.args, equalTo(mapOf("foo" to mapValue)))
        assertThat(requiredLens(requiredInjected), equalTo(value))
        assertThrows<LensFailure> { requiredLens(ToolRequest()) }

        val defaultedLens = spec.defaulted("foo", value, "bar")
        val defaultedInjected = ToolRequest().with(defaultedLens of value)

        assertThat(defaultedInjected.args, equalTo(mapOf("foo" to mapValue)))
        assertThat(defaultedLens(defaultedInjected), equalTo(value))
        assertThat(defaultedLens(ToolRequest()), equalTo(value))

        val checkSchemaLens = spec.required("foo", "bar", mapOf("int" to 1, "string" to "test", "bool" to true))

        assertApproved(McpJson.asFormatString(checkSchemaLens.toSchema()), APPLICATION_JSON)
    }
}
