package org.http4k.connect.kafka.rest

import http4k.RandomKey
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(ApprovalTest::class)
class SchemaRegistryMoshiTest {

    @Test
    fun `can serialise schema`(approver: Approver) {
        val input = RandomKey(UUID.randomUUID()).schema
        approver.assertApproved(SchemaRegistryMoshi.asFormatString(input))
    }
}
