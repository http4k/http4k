package org.http4k.contract.jsonschema

import org.http4k.contract.jsonschema.v3.ArbObject
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.Jackson
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class SchemaCollapserTest {

    @Test
    fun `can collapse schema by inlining refs`(approver: Approver) {
        val original = AutoJsonToJsonSchema(Jackson).toSchema(ArbObject())
        approver.assertApproved(Jackson.pretty(SchemaCollapser(Jackson).collapseToNode(original)), APPLICATION_JSON)
    }
}
