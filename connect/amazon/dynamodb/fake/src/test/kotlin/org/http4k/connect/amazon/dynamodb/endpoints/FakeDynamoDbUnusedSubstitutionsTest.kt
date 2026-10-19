package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.batchGetItem
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.deleteItem
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.ReqGetItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.query
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.scan
import org.http4k.connect.amazon.dynamodb.updateItem
import org.http4k.connect.successValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** DynamoDB refuses a request that supplies an expression attribute name or value which no expression uses. */
class FakeDynamoDbUnusedSubstitutionsTest : DynamoDbSource by FakeDynamoDbSource() {

    private val table = TableName.sample()
    private val key = Key(attrS of "hash1")

    private fun unusedNames(keys: String) =
        """{"__type":"com.amazon.coral.validate#ValidationException","Message":"Value provided in ExpressionAttributeNames unused in expressions: keys: {$keys}"}"""

    private fun unusedValues(keys: String) =
        """{"__type":"com.amazon.coral.validate#ValidationException","Message":"Value provided in ExpressionAttributeValues unused in expressions: keys: {$keys}"}"""

    @BeforeEach
    fun setUp() {
        dynamo.createTable(table, attrS)
        dynamo.putItem(table, Item(attrS of "hash1", attrN of 1)).successValue()
    }

    @Test
    fun `scan with an unused name or value`() {
        assertThat(
            dynamo.scan(
                TableName = table,
                FilterExpression = "#n = :v",
                ExpressionAttributeNames = mapOf("#n" to attrN.name, "#unused" to AttributeName.of("other")),
                ExpressionAttributeValues = mapOf(":v" to attrN.asValue(1))
            ).failureOrNull()?.message,
            equalTo(unusedNames("#unused"))
        )

        assertThat(
            dynamo.scan(
                TableName = table,
                FilterExpression = "$attrN = :v",
                ExpressionAttributeValues = mapOf(":v" to attrN.asValue(1), ":a" to attrN.asValue(2), ":b" to attrN.asValue(3))
            ).failureOrNull()?.message,
            equalTo(unusedValues(":a, :b"))
        )
    }

    @Test
    fun `query counts the key condition, filter and projection`() {
        dynamo.query(
            TableName = table,
            KeyConditionExpression = "#k = :k",
            FilterExpression = "#f = :f",
            ProjectionExpression = "#p",
            ExpressionAttributeNames = mapOf("#k" to attrS.name, "#f" to attrN.name, "#p" to attrS.name),
            ExpressionAttributeValues = mapOf(":k" to attrS.asValue("hash1"), ":f" to attrN.asValue(1))
        ).successValue()

        assertThat(
            dynamo.query(
                TableName = table,
                KeyConditionExpression = "$attrS = :k",
                ExpressionAttributeValues = mapOf(":k" to attrS.asValue("hash1"), ":f" to attrN.asValue(1))
            ).failureOrNull()?.message,
            equalTo(unusedValues(":f"))
        )
    }

    @Test
    fun `get item with a name its projection does not use`() {
        assertThat(
            dynamo.getItem(
                TableName = table,
                Key = key,
                ProjectionExpression = "#p",
                ExpressionAttributeNames = mapOf("#p" to attrS.name, "#unused" to attrN.name)
            ).failureOrNull()?.message,
            equalTo(unusedNames("#unused"))
        )
    }

    @Test
    fun `batch get item with a name its projection does not use`() {
        assertThat(
            dynamo.batchGetItem(
                mapOf(table to ReqGetItem.Get(listOf(key), ExpressionAttributeNames = mapOf("#unused" to attrN.name)))
            ).failureOrNull()?.message,
            equalTo(unusedNames("#unused"))
        )
    }

    @Test
    fun `writes with an unused name or value are refused and change nothing`() {
        assertThat(
            dynamo.putItem(
                TableName = table,
                Item = Item(attrS of "hash1", attrN of 2),
                ExpressionAttributeValues = mapOf(":unused" to attrN.asValue(1))
            ).failureOrNull()?.message,
            equalTo(unusedValues(":unused"))
        )

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "SET $attrN = :v",
                ConditionExpression = "attribute_exists(#s)",
                ExpressionAttributeNames = mapOf("#s" to attrS.name, "#unused" to attrN.name),
                ExpressionAttributeValues = mapOf(":v" to attrN.asValue(3))
            ).failureOrNull()?.message,
            equalTo(unusedNames("#unused"))
        )

        assertThat(
            dynamo.deleteItem(
                TableName = table,
                Key = key,
                ExpressionAttributeNames = mapOf("#unused" to attrN.name)
            ).failureOrNull()?.message,
            equalTo(unusedNames("#unused"))
        )

        assertThat(dynamo.getItem(table, key).successValue().item?.get(attrN.name), equalTo(attrN.asValue(1)))
    }

    @Test
    fun `update item counts both the update and the condition expression`() {
        dynamo.updateItem(
            TableName = table,
            Key = key,
            UpdateExpression = "SET #n = :v",
            ConditionExpression = "#n = :old",
            ExpressionAttributeNames = mapOf("#n" to attrN.name),
            ExpressionAttributeValues = mapOf(":v" to attrN.asValue(5), ":old" to attrN.asValue(1))
        ).successValue()
    }
}
