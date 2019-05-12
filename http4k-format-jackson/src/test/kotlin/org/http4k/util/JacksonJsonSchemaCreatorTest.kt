package org.http4k.util

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asJsonString
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

data class Simple(val nested: List<Nested>)
data class Nested(val int: Int)

@ExtendWith(JsonApprovalTest::class)
class JacksonJsonSchemaCreatorTest {

    @Test
    fun `generates schema for normal object`(approver: Approver) {
        approver.assertApproved(Simple(listOf(Nested(123))))
    }

    @Test
    @Disabled
    fun `generates schema for list`(approver: Approver) {
        approver.assertApproved(listOf(Nested(123)))
    }

    private fun Approver.assertApproved(obj: Any) {
        assertApproved(Response(OK)
            .with(CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(asJsonString(JacksonJsonSchemaCreator(Jackson).toSchema(obj))))
    }
}