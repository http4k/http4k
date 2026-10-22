package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.batchGetItem
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.deleteItem
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.ReqGetItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TransactGetItem
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.query
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.scan
import org.http4k.connect.amazon.dynamodb.transactGetItems
import org.http4k.connect.amazon.dynamodb.transactWriteItems
import org.http4k.connect.amazon.dynamodb.updateItem
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * DynamoDB refuses expression attribute names and values which don't fit the request's expressions:
 * supplied with no expression at all, empty, or holding a token no expression refers to.
 */
abstract class DynamoDbExpressionAttributesContract : DynamoDbSource {

    private val table = TableName.sample()
    private val missingTable = TableName.sample()
    private val key = Key(attrS of "hash1")
    private val unusedName = mapOf("#unused" to AttributeName.of("other"))
    private val unusedValue = mapOf(":unused" to attrN.asValue(9))

    @BeforeEach
    fun createTable() {
        dynamo.createTable(
            table,
            KeySchema = KeySchema.compound(attrS.name),
            AttributeDefinitions = listOf(attrS.asAttributeDefinition()),
            BillingMode = BillingMode.PAY_PER_REQUEST
        ).successValue()

        dynamo.waitForExist(table)
        dynamo.putItem(table, Item(attrS of "hash1", attrN of 1)).successValue()
    }

    @Test
    fun `unused names and values are refused by every expression-taking call`() {
        dynamo.getItem(table, key, ProjectionExpression = "#p", ExpressionAttributeNames = mapOf("#p" to attrS.name) + unusedName)
            .assertUnusedNames("#unused")

        dynamo.batchGetItem(
            mapOf(table to ReqGetItem.Get(listOf(key), ProjectionExpression = "#p", ExpressionAttributeNames = mapOf("#p" to attrS.name) + unusedName))
        ).assertUnusedNames("#unused")

        dynamo.putItem(
            TableName = table,
            Item = Item(attrS of "hash1", attrN of 2),
            ConditionExpression = "attribute_exists($attrS)",
            ExpressionAttributeValues = unusedValue
        ).assertUnusedValues(":unused")

        dynamo.updateItem(
            TableName = table,
            Key = key,
            UpdateExpression = "SET $attrN = :v",
            ConditionExpression = "attribute_exists(#s)",
            ExpressionAttributeNames = mapOf("#s" to attrS.name) + unusedName,
            ExpressionAttributeValues = mapOf(":v" to attrN.asValue(3))
        ).assertUnusedNames("#unused")

        dynamo.deleteItem(
            TableName = table,
            Key = key,
            ConditionExpression = "attribute_exists($attrS)",
            ExpressionAttributeNames = unusedName
        ).assertUnusedNames("#unused")

        dynamo.query(
            TableName = table,
            KeyConditionExpression = "$attrS = :k",
            ExpressionAttributeValues = mapOf(":k" to attrS.asValue("hash1")) + unusedValue
        ).assertUnusedValues(":unused")

        dynamo.scan(
            TableName = table,
            FilterExpression = "#n = :v",
            ExpressionAttributeNames = mapOf("#n" to attrN.name),
            ExpressionAttributeValues = mapOf(":v" to attrN.asValue(1), ":a" to attrN.asValue(2), ":b" to attrN.asValue(3))
        ).assertUnusedValues(":a, :b")

        assertThat(dynamo.getItem(table, key).successValue().item?.get(attrN.name), equalTo(attrN.asValue(1)))
    }

    @Test
    fun `every expression of a call counts as a use`() {
        dynamo.query(
            TableName = table,
            KeyConditionExpression = "#k = :k",
            FilterExpression = "#f = :f",
            ProjectionExpression = "#p",
            ExpressionAttributeNames = mapOf("#k" to attrS.name, "#f" to attrN.name, "#p" to attrS.name),
            ExpressionAttributeValues = mapOf(":k" to attrS.asValue("hash1"), ":f" to attrN.asValue(1))
        ).successValue()

        dynamo.updateItem(
            TableName = table,
            Key = key,
            UpdateExpression = "SET #n = :v",
            ConditionExpression = "#n = :old",
            ExpressionAttributeNames = mapOf("#n" to attrN.name),
            ExpressionAttributeValues = mapOf(":v" to attrN.asValue(5), ":old" to attrN.asValue(1))
        ).successValue()
    }

    @Test
    fun `a token which only prefixes a used one is unused`() {
        dynamo.putItem(
            TableName = table,
            Item = Item(attrS of "hash1"),
            ConditionExpression = "attribute_exists(#ab)",
            ExpressionAttributeNames = mapOf("#a" to attrN.name, "#ab" to attrS.name)
        ).assertUnusedNames("#a")
    }

    @Test
    fun `unused names are reported before unused values`() {
        dynamo.putItem(
            TableName = table,
            Item = Item(attrS of "hash1"),
            ConditionExpression = "attribute_exists($attrS)",
            ExpressionAttributeNames = unusedName,
            ExpressionAttributeValues = unusedValue
        ).assertUnusedNames("#unused")
    }

    @Test
    fun `names are refused when the call has no expression at all`() {
        val message = "ExpressionAttributeNames can only be specified when using expressions"

        dynamo.getItem(table, key, ExpressionAttributeNames = unusedName).assertValidationError(message)
        dynamo.batchGetItem(mapOf(table to ReqGetItem.Get(listOf(key), ExpressionAttributeNames = unusedName)))
            .assertValidationError(message)
        dynamo.putItem(table, Item(attrS of "hash1"), ExpressionAttributeNames = unusedName).assertValidationError(message)
        dynamo.deleteItem(table, key, ExpressionAttributeNames = unusedName).assertValidationError(message)
        dynamo.updateItem(table, key, ExpressionAttributeNames = unusedName).assertValidationError(message)
        dynamo.scan(table, ExpressionAttributeNames = unusedName).assertValidationError(message)
    }

    @Test
    fun `values are refused when the call has no expression which takes them`() {
        val message = "ExpressionAttributeValues can only be specified when using expressions"

        dynamo.putItem(table, Item(attrS of "hash1"), ExpressionAttributeValues = unusedValue)
            .assertValidationError("$message: ConditionExpression is null")
        dynamo.deleteItem(table, key, ExpressionAttributeValues = unusedValue)
            .assertValidationError("$message: ConditionExpression is null")
        dynamo.updateItem(table, key, ExpressionAttributeValues = unusedValue)
            .assertValidationError("$message: UpdateExpression and ConditionExpression are null")
        dynamo.scan(table, ProjectionExpression = "$attrS", ExpressionAttributeValues = unusedValue)
            .assertValidationError("$message: FilterExpression is null")
    }

    @Test
    fun `empty names or values are refused`() {
        dynamo.putItem(
            TableName = table,
            Item = Item(attrS of "hash1"),
            ConditionExpression = "attribute_exists($attrS)",
            ExpressionAttributeNames = emptyMap()
        ).assertValidationError("ExpressionAttributeNames must not be empty")

        dynamo.putItem(
            TableName = table,
            Item = Item(attrS of "hash1"),
            ConditionExpression = "attribute_exists($attrS)",
            ExpressionAttributeValues = emptyMap()
        ).assertValidationError("ExpressionAttributeValues must not be empty")
    }

    @Test
    fun `a missing table is reported before the substitutions`() {
        dynamo.putItem(
            TableName = missingTable,
            Item = Item(attrS of "hash1"),
            ConditionExpression = "attribute_exists($attrS)",
            ExpressionAttributeValues = unusedValue
        ).assertResourceNotFound()

        dynamo.getItem(missingTable, key, ProjectionExpression = "$attrS", ExpressionAttributeNames = unusedName)
            .assertResourceNotFound()
    }

    @Test
    fun `transactions refuse unused names and values in any item`() {
        dynamo.transactWriteItems(
            listOf(
                TransactWriteItem.Put(
                    TableName = table,
                    Item = Item(attrS of "hash2"),
                    ConditionExpression = "attribute_not_exists($attrS)",
                    ExpressionAttributeValues = unusedValue
                )
            )
        ).assertUnusedValues(":unused")

        dynamo.transactWriteItems(
            listOf(
                TransactWriteItem.Update(
                    TableName = table,
                    Key = key,
                    UpdateExpression = "SET $attrN = :v",
                    ExpressionAttributeValues = mapOf(":v" to attrN.asValue(3)) + unusedValue
                )
            )
        ).assertUnusedValues(":unused")

        dynamo.transactWriteItems(
            listOf(
                TransactWriteItem.Delete(
                    TableName = table,
                    Key = key,
                    ConditionExpression = "attribute_exists($attrS)",
                    ExpressionAttributeNames = unusedName
                )
            )
        ).assertUnusedNames("#unused")

        dynamo.transactWriteItems(
            listOf(
                TransactWriteItem.ConditionCheck(
                    TableName = table,
                    Key = key,
                    ConditionExpression = "attribute_exists($attrS)",
                    ExpressionAttributeNames = unusedName
                )
            )
        ).assertUnusedNames("#unused")

        dynamo.transactGetItems(
            listOf(TransactGetItem.Get(table, key, ProjectionExpression = "$attrS", ExpressionAttributeNames = unusedName))
        ).assertUnusedNames("#unused")

        assertThat(dynamo.getItem(table, key).successValue().item?.get(attrN.name), equalTo(attrN.asValue(1)))
    }
}

private fun Result4k<*, RemoteFailure>.assertUnusedNames(keys: String) =
    assertValidationError("Value provided in ExpressionAttributeNames unused in expressions: keys: {$keys}")

private fun Result4k<*, RemoteFailure>.assertUnusedValues(keys: String) =
    assertValidationError("Value provided in ExpressionAttributeValues unused in expressions: keys: {$keys}")

private fun Result4k<*, RemoteFailure>.assertValidationError(message: String) {
    val error = assertBadRequest()
    assertThat(error.__type, equalTo("com.amazon.coral.validate#ValidationException"))
    assertThat(error.Message, equalTo(message))
}

/** The fake and DynamoDB Local word this one differently, so only its kind is compared. */
private fun Result4k<*, RemoteFailure>.assertResourceNotFound() {
    assertThat(assertBadRequest().__type, endsWith("ResourceNotFoundException"))
}

private fun Result4k<*, RemoteFailure>.assertBadRequest(): JsonError {
    val failure = failureOrNull()!!
    assertThat(failure.status, equalTo(BAD_REQUEST))
    return DynamoDbMoshi.asA(failure.message!!, JsonError::class)
}
