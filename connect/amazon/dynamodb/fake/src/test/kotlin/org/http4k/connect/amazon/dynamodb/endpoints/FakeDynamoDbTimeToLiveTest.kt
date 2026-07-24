package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.deleteTable
import org.http4k.connect.amazon.dynamodb.describeTimeToLive
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveSpecification
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveStatus.DISABLED
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveStatus.ENABLED
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.updateTimeToLive
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

// Fake-only: disabling and delete/recreate cannot go in the shared DynamoDbContract because real AWS rejects
// a second UpdateTimeToLive within the (up to one hour) processing window. The fake's instant-transition
// model lets us assert the state machine directly here.
class FakeDynamoDbTimeToLiveTest {
    private val dynamo = FakeDynamoDb().client()
    private val ttlAttribute = AttributeName.of("expiresAt")

    @Test
    fun `enabling records the attribute, disabling clears it`() {
        val table = TableName.sample()
        dynamo.createTable(table, attrS)

        dynamo.updateTimeToLive(table, TimeToLiveSpecification(Enabled = true, AttributeName = ttlAttribute)).successValue()
        val enabled = dynamo.describeTimeToLive(table).successValue().TimeToLiveDescription
        assertThat(enabled.TimeToLiveStatus, equalTo(ENABLED))
        assertThat(enabled.AttributeName, equalTo(ttlAttribute))

        dynamo.updateTimeToLive(table, TimeToLiveSpecification(Enabled = false, AttributeName = ttlAttribute)).successValue()
        val disabled = dynamo.describeTimeToLive(table).successValue().TimeToLiveDescription
        assertThat(disabled.TimeToLiveStatus, equalTo(DISABLED))
        assertThat(disabled.AttributeName, absent())
    }

    @Test
    fun `a never-configured table reports DISABLED with no attribute`() {
        val table = TableName.sample()
        dynamo.createTable(table, attrS)

        val ttl = dynamo.describeTimeToLive(table).successValue().TimeToLiveDescription
        assertThat(ttl.TimeToLiveStatus, equalTo(DISABLED))
        assertThat(ttl.AttributeName, absent())
    }

    @Test
    fun `TTL config is dropped with the table, so a recreated table starts disabled`() {
        val table = TableName.sample()
        dynamo.createTable(table, attrS)
        dynamo.updateTimeToLive(table, TimeToLiveSpecification(Enabled = true, AttributeName = ttlAttribute)).successValue()

        dynamo.deleteTable(table).successValue()
        dynamo.createTable(table, attrS)

        val fresh = dynamo.describeTimeToLive(table).successValue().TimeToLiveDescription
        assertThat(fresh.TimeToLiveStatus, equalTo(DISABLED))
        assertThat(fresh.AttributeName, absent())
    }
}
