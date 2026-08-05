package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.action.ConditionalCheckFailed
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.successValue
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class DynamoDbPutItemContract : DynamoDbSource {

    private val table = TableName.sample()

    @BeforeEach
    fun createTable() {
        dynamo.createTable(
            table,
            KeySchema = KeySchema.compound(attrS.name),
            AttributeDefinitions = listOf(attrS.asAttributeDefinition()),
            BillingMode = BillingMode.PAY_PER_REQUEST
        ).successValue()

        dynamo.waitForExist(table)
    }

    @Test
    fun `put item`() {
        val item = Item(attrS of "hash1", attrN of 1)

        dynamo.putItem(table, item)

        assertThat(
            dynamo.getItem(table, Key(attrS of "hash1")).successValue().item,
            equalTo(item)
        )
    }

    @Test
    fun `replace item`() {
        val original = Item(attrS of "hash1", attrN of 1)
        val updated = Item(attrS of "hash1", attrN of 2)

        dynamo.putItem(table, original)
        dynamo.putItem(table, updated)

        assertThat(
            dynamo.getItem(table, Key(attrS of "hash1")).successValue().item,
            equalTo(updated)
        )
    }

    @Test
    fun `conditional put item - key does not exist`() {
        val item = Item(attrS of "hash1", attrN of 1)

        dynamo.putItem(table, item).successValue()

        dynamo.putItem(
            TableName = table,
            Item = item,
            ConditionExpression = "attribute_not_exists(#key1)",
            ExpressionAttributeNames = mapOf("#key1" to attrS.name)
        ).assertConditionCheckFailed()
    }

    @Test
    fun `conditional put item - comparison`() {
        val item = Item(attrS of "hash1", attrN of 1)

        dynamo.putItem(table, item).successValue()

        dynamo.putItem(
            TableName = table,
            Item = item,
            ConditionExpression = "#key1 > :val1",
            ExpressionAttributeNames = mapOf("#key1" to attrN.name),
            ExpressionAttributeValues = mapOf(":val1" to attrN.asValue(1))
        ).assertConditionCheckFailed()

        dynamo.putItem(
            TableName = table,
            Item = item,
            ConditionExpression = "#key1 > :val1",
            ExpressionAttributeNames = mapOf("#key1" to attrN.name),
            ExpressionAttributeValues = mapOf(":val1" to attrN.asValue(0))
        ).successValue()
    }

    @Test
    fun `conditional put item - on missing item`() {
        dynamo.putItem(
            TableName = table,
            Item = Item(attrS of "hash1", attrN of 1),
            ConditionExpression = "attribute_exists(#key1)",
            ExpressionAttributeNames = mapOf("#key1" to attrS.name)
        ).assertConditionCheckFailed()
    }
}

/**
 * Asserted through the model rather than as raw JSON because DynamoDB and DynamoDB Local spell the
 * message field differently, and both run this contract.
 */
private fun Result4k<*, RemoteFailure>.assertConditionCheckFailed() {
    val failure = failureOrNull()!!

    assertThat(failure.method, equalTo(Method.POST))
    assertThat(failure.uri, equalTo(Uri.of("/")))
    assertThat(failure.status, equalTo(Status.BAD_REQUEST))

    with(DynamoDbMoshi.asA(failure.message!!, ConditionalCheckFailed::class)) {
        assertThat(__type, equalTo("com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException"))
        assertThat(Message, equalTo("The conditional request failed"))
    }
}
