package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.attrNL
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.deleteItem
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Put
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.scan
import org.http4k.connect.amazon.dynamodb.transactWriteItems
import org.http4k.connect.amazon.dynamodb.updateItem
import org.http4k.connect.successValue
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * `attribute_type(#a, :t)` - the form real DynamoDB requires for the type operand - over the endpoints
 * which evaluate a condition. Fake-only rather than in the shared contracts because the malformed-operand
 * cases assert the fake's own `ValidationException` message.
 */
class FakeDynamoDbAttributeTypeTest {

    private val dynamo = FakeDynamoDb().client()
    private val table = TableName.sample()
    private val key = Key(attrS of "hash1")

    private val claimName = mapOf("#claim" to attrNL.name)
    private val claimValues = mapOf(
        ":nulltype" to AttributeValue.Str("NULL"),
        ":owner" to AttributeValue.Str("owner1")
    )

    // claim-if-unclaimed: the row is free when the claim attribute is absent or holds an explicit NULL
    private val unclaimed = "attribute_not_exists(#claim) OR attribute_type(#claim, :nulltype)"

    private val undefinedNullType =
        "An expression attribute value used in expression is not defined; attribute value: :nulltype"

    @BeforeEach
    fun createTable() {
        dynamo.createTable(table, attrS)
    }

    @Test
    fun `claim applies when the attribute is absent`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        dynamo.updateItem(
            TableName = table,
            Key = key,
            UpdateExpression = "SET #claim = :owner",
            ConditionExpression = unclaimed,
            ExpressionAttributeNames = claimName,
            ExpressionAttributeValues = claimValues
        ).successValue()

        assertThat(storedClaim(), equalTo(AttributeValue.Str("owner1")))
    }

    @Test
    fun `claim applies when the attribute holds an explicit NULL`() {
        dynamo.putItem(table, Item(attrS of "hash1", attrNL of null)).successValue()

        dynamo.updateItem(
            TableName = table,
            Key = key,
            UpdateExpression = "SET #claim = :owner",
            ConditionExpression = unclaimed,
            ExpressionAttributeNames = claimName,
            ExpressionAttributeValues = claimValues
        ).successValue()

        assertThat(storedClaim(), equalTo(AttributeValue.Str("owner1")))
    }

    @Test
    fun `claim is refused when the attribute already holds a value`() {
        dynamo.putItem(table, Item(attrS of "hash1", attrNL of "owner0")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "SET #claim = :owner",
                ConditionExpression = unclaimed,
                ExpressionAttributeNames = claimName,
                ExpressionAttributeValues = claimValues
            ).failureOrNull(),
            equalTo(
                RemoteFailure(
                    method = Method.POST,
                    uri = Uri.of("/"),
                    status = Status.BAD_REQUEST,
                    message = """{"__type":"com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException","message":"The conditional request failed"}"""
                )
            )
        )

        assertThat(storedClaim(), equalTo(AttributeValue.Str("owner0")))
    }

    @Test
    fun `bare type name is still accepted on the condition path`() {
        dynamo.putItem(table, Item(attrS of "hash1", attrNL of null)).successValue()

        dynamo.updateItem(
            TableName = table,
            Key = key,
            UpdateExpression = "SET #claim = :owner",
            ConditionExpression = "attribute_type(#claim, NULL)",
            ExpressionAttributeNames = claimName,
            ExpressionAttributeValues = claimValues
        ).successValue()

        assertThat(storedClaim(), equalTo(AttributeValue.Str("owner1")))
    }

    @Test
    fun `attribute type filters a scan`() {
        val claimed = Item(attrS of "hash1", attrNL of "owner0")
        dynamo.putItem(table, claimed).successValue()
        dynamo.putItem(table, Item(attrS of "hash2", attrNL of null)).successValue()

        val result = dynamo.scan(
            TableName = table,
            FilterExpression = "attribute_type(#claim, :type)",
            ExpressionAttributeNames = claimName,
            ExpressionAttributeValues = mapOf(":type" to AttributeValue.Str("S"))
        ).successValue()

        assertThat(result.items, hasSize(equalTo(1)))
        assertThat(result.items, hasElement(claimed))
    }

    @Test
    fun `undefined type value on update is reported as a validation error, not a server error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "REMOVE #claim",
                ConditionExpression = "attribute_type(#claim, :nulltype)",
                ExpressionAttributeNames = claimName
            ).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", undefinedNullType))
        )
    }

    @Test
    fun `undefined type value in a scan filter is reported as a validation error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.scan(
                TableName = table,
                FilterExpression = "attribute_type(#claim, :nulltype)",
                ExpressionAttributeNames = claimName
            ).failureOrNull(),
            equalTo(validationFailure("FilterExpression", undefinedNullType))
        )
    }

    // regression: eval short-circuits `OR`, so a bad token in the right operand used to go unreported
    // whenever the left operand happened to be true. DynamoDB rejects the request whatever the data says.
    @Test
    fun `undefined type value is reported even when the other side of an OR is true`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "SET #claim = :owner",
                ConditionExpression = unclaimed,
                ExpressionAttributeNames = claimName,
                ExpressionAttributeValues = mapOf(":owner" to AttributeValue.Str("owner1"))
            ).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", undefinedNullType))
        )

        assertThat(storedClaim(), absent())
    }

    @Test
    fun `undefined type value is reported even when the other side of an AND is false`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "REMOVE #claim",
                ConditionExpression = "attribute_exists(#claim) AND attribute_type(#claim, :nulltype)",
                ExpressionAttributeNames = claimName
            ).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", undefinedNullType))
        )
    }

    @Test
    fun `undefined type value under a NOT is reported`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "REMOVE #claim",
                ConditionExpression = "NOT attribute_type(#claim, :nulltype)",
                ExpressionAttributeNames = claimName
            ).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", undefinedNullType))
        )
    }

    @Test
    fun `bad type value on a conditional put is a validation error, not a server error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(putWithType(null).failureOrNull(), equalTo(validationFailure("ConditionExpression", undefinedNullType)))
        assertThat(
            putWithType(AttributeValue.Num(123)).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", invalidTypeName("AttributeValue(N=123)")))
        )
        assertThat(
            putWithType(AttributeValue.Str("banana")).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", invalidTypeName("banana")))
        )
    }

    @Test
    fun `bad type value on a conditional delete is a validation error, not a server error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(deleteWithType(null).failureOrNull(), equalTo(validationFailure("ConditionExpression", undefinedNullType)))
        assertThat(
            deleteWithType(AttributeValue.Str("banana")).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", invalidTypeName("banana")))
        )

        // the guarded record is still there - the request never reached the write
        assertThat(dynamo.getItem(table, key).successValue().item, present())
    }

    @Test
    fun `bad type value in a transaction is a validation error, not a server error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.transactWriteItems(
                listOf(
                    Put(
                        TableName = table,
                        Item = Item(attrS of "hash1", attrNL of "owner1"),
                        ConditionExpression = "attribute_type(#claim, :nulltype)",
                        ExpressionAttributeNames = claimName
                    )
                )
            ).failureOrNull(),
            equalTo(validationFailure("ConditionExpression", undefinedNullType))
        )

        assertThat(storedClaim(), absent())
    }

    // the same request-error mapping, for the other two substitution references a condition can name
    @Test
    fun `undefined expression attribute value on a write is a validation error, not a server error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "REMOVE #claim",
                ConditionExpression = "#claim = :missing",
                ExpressionAttributeNames = claimName
            ).failureOrNull(),
            equalTo(
                validationFailure(
                    "ConditionExpression",
                    "An expression attribute value used in expression is not defined; attribute value: :missing"
                )
            )
        )
    }

    @Test
    fun `undefined name in a branch the evaluation never reaches is a validation error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.updateItem(
                TableName = table,
                Key = key,
                UpdateExpression = "REMOVE #claim",
                ConditionExpression = "attribute_not_exists(absent) OR #bad = :undefined",
                ExpressionAttributeNames = claimName
            ).failureOrNull(),
            equalTo(
                validationFailure(
                    "ConditionExpression",
                    "An expression attribute name used in the document path is not defined; attribute name: #bad"
                )
            )
        )
    }

    @Test
    fun `undefined expression attribute name in a scan filter is a validation error`() {
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()

        assertThat(
            dynamo.scan(TableName = table, FilterExpression = "attribute_exists(#nope)").failureOrNull(),
            equalTo(
                validationFailure(
                    "FilterExpression",
                    "An expression attribute name used in the document path is not defined; attribute name: #nope"
                )
            )
        )
    }

    private fun putWithType(type: AttributeValue?) = dynamo.putItem(
        TableName = table,
        Item = Item(attrS of "hash1", attrNL of "owner1"),
        ConditionExpression = "attribute_type(#claim, :nulltype)",
        ExpressionAttributeNames = claimName,
        ExpressionAttributeValues = type?.let { mapOf(":nulltype" to it) }
    )

    private fun deleteWithType(type: AttributeValue?) = dynamo.deleteItem(
        TableName = table,
        Key = key,
        ConditionExpression = "attribute_type(#claim, :nulltype)",
        ExpressionAttributeNames = claimName,
        ExpressionAttributeValues = type?.let { mapOf(":nulltype" to it) }
    )

    private fun invalidTypeName(type: String) =
        "Invalid attribute type name found; type: $type, valid types: {B,BOOL,BS,L,M,N,NS,NULL,S,SS}"

    private fun validationFailure(field: String, message: String) = RemoteFailure(
        method = Method.POST,
        uri = Uri.of("/"),
        status = Status.BAD_REQUEST,
        message = """{"__type":"com.amazon.coral.validate#ValidationException","Message":"Invalid $field: $message"}"""
    )

    private fun storedClaim() = dynamo.getItem(table, key).successValue().item?.get(attrNL.name)
}
