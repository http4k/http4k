package org.http4k.util

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asJsonString
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

data class Simple(val nested: List<Nested>)
data class Nested(val int: Int)

@ExtendWith(ApprovalTest::class)
class JacksonJsonSchemaCreatorTest {

    @Test
    fun `generates schema for simple object`(approver: Approver) {
        assertApproved(approver, Simple(listOf(Nested(123))))
    }

    private fun assertApproved(approver: Approver, obj: Simple) {
        approver.assertApproved(Response(OK).body(asJsonString(JacksonJsonSchemaCreator(Jackson).toSchema(obj))))
    }
}