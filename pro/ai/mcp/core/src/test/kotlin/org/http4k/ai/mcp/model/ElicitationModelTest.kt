package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.model.Elicitation.Metadata.EnumNames
import org.http4k.ai.mcp.model.Elicitation.Metadata.string.MaxLength
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ElicitationModelTest {
    @Test
    fun `creates schema`(approver: Approver) {
        approver.assertApproved(McpJson.asFormatString(Foo().toSchema()), APPLICATION_JSON)
    }

    @Test
    fun `represent as string`() {
        assert(Foo().apply {
            s = "asd"
            i = 123
            d = 1.23
            b = true
            e = FooEnum.A
        }.toString() == "Foo(b=true, d=1.23, e=A, i=123, l=null, ob=null, od=null, oe=null, oi=null, ol=null, os=null, s=asd)")
    }
}

class Foo : ElicitationModel() {
    var s by string("s", "the s", MaxLength(10))
    var os by optionalString("os", "the os")
    var e by enum("e", "the e", EnumNames(FooEnum.entries.associateWith { it.name.lowercase() }))
    var oe by optionalEnum<FooEnum>("oe", "the oe")
    var l by long("l", "the l")
    var ol by optionalLong("ol", "the ol")
    var i by int("i", "the i")
    var oi by optionalInt("oi", "the oi")
    var d by double("d", "the d")
    var od by optionalDouble("od", "the od")
    var b by boolean("b", "the b")
    var ob by optionalBoolean("ob", "the ob")
}

enum class FooEnum {
    A, B, C
}
