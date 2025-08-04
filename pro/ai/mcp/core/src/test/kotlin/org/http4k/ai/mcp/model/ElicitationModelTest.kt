package org.http4k.ai.mcp.model

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
        assert(Foo().apply { foo = "asd" }.toString() == "Foo(bar=null, foo=asd)")
    }
}

class Foo : ElicitationModel() {
    var foo by string("foo", "the foo")
    var bar by optionalString("bar", "the bar")
}
