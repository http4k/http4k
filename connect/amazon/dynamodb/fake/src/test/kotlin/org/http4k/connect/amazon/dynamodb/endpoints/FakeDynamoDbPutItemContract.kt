package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class FakeDynamoDbPutItemContract : DynamoDbPutItemContract(), DynamoDbSource by FakeDynamoDbSource() {

    /**
     * The shared contract asserts through the model, which accepts either spelling of the message field
     * because DynamoDB Local answers with the other one. The fake answers as DynamoDB itself does, so
     * its wire body is pinned here instead.
     */
    @Test
    fun `condition failure is reported with DynamoDB's own spelling of the message field`() {
        val ownTable = TableName.sample()
        dynamo.createTable(ownTable, attrS)

        val item = Item(attrS of "hash1")
        dynamo.putItem(ownTable, item).successValue()

        assertThat(
            dynamo.putItem(
                TableName = ownTable,
                Item = item,
                ConditionExpression = "attribute_not_exists(#key1)",
                ExpressionAttributeNames = mapOf("#key1" to attrS.name)
            ).failureOrNull()?.message,
            equalTo("""{"__type":"com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException","message":"The conditional request failed"}""")
        )
    }
}
