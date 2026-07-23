package org.http4k.connect.amazon.dynamodb

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.hasElement
import dev.forkhandles.result4k.valueOrNull
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.dynamodb.action.ConditionalCheckFailed
import org.http4k.connect.amazon.dynamodb.action.Scan
import org.http4k.connect.amazon.dynamodb.action.copy
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.List
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Null
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Num
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Str
import org.http4k.connect.amazon.dynamodb.model.BillingMode.PROVISIONED
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.ProvisionedThroughput
import org.http4k.connect.amazon.dynamodb.model.ReqGetItem
import org.http4k.connect.amazon.dynamodb.model.ReqStatement
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem.Companion.Put
import org.http4k.connect.amazon.dynamodb.model.ReturnConsumedCapacity.TOTAL
import org.http4k.connect.amazon.dynamodb.model.ReturnValuesOnConditionCheckFailure.ALL_OLD
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveSpecification
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveStatus.DISABLED
import org.http4k.connect.amazon.dynamodb.model.TransactGetItem.Companion.Get
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Delete
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Put
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Update
import org.http4k.connect.amazon.dynamodb.model.toItem
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.connect.failureValue
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class MyValueType(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<MyValueType>(::MyValueType)
}

/**
 * DynamoDB reports a failed condition as an error body rather than a typed response, so the item it
 * returns has to be read back out of that body.
 */
private fun RemoteFailure.conditionCheckFailureItem() =
    DynamoDbMoshi.asA(message!!, ConditionalCheckFailed::class).Item?.toItem()

interface DynamoDbContract : AwsContract {
    val duration: Duration

    private val dynamo get() = DynamoDb.Http(aws.region, { aws.credentials }, http)

    val table get() = TableName.sample(suffix = uuid(0).toString())

    @BeforeEach
    fun create() {
        assertThat(dynamo.createTable(table, attrS).TableDescription.ItemCount, equalTo(0))
        waitForUpdate()
    }

    @AfterEach
    fun after() {
        dynamo.deleteTable(table)
    }

    @Test
    open fun `transactional items`() {
        with(dynamo) {
            transactWriteItems(
                listOf(
                    Put(table, Item(attrS of "hello5"))
                )
            ).successValue()

            transactWriteItems(
                listOf(
                    Update(
                        table,
                        Item(attrS of "hello"),
                        "SET $attrBool = :c",
                        ExpressionAttributeValues = mapOf(":c" to attrBool.asValue(true))
                    ),
                    Put(table, createItem("hello2")),
                    Put(table, createItem("hello3")),
                    Delete(table, Item(attrS of "hello4")),
                    Delete(table, Item(attrS of "hello5"))
                )
            ).successValue()

            val result = transactGetItems(
                listOf(
                    Get(table, Item(attrS of "hello2")),
                    Get(table, Item(attrS of "hello3")),
                    Get(table, Item(attrS of "hello4"))
                )
            ).successValue()
            assertThat(attrS(result.responses[0]!!), equalTo("hello2"))
            assertThat(attrS(result.responses[1]!!), equalTo("hello3"))
            assertThat(result.responses[2], absent())
        }
    }

    @Test
    fun `batch operations`() {
        with(dynamo) {
            val write = batchWriteItem(
                mapOf(
                    table to listOf(
                        Put(createItem("hello2")),
                        ReqWriteItem.Delete(Item(attrS of "hello"))
                    )
                )
            ).successValue()

            assertThat(write.UnprocessedItems, equalTo(emptyMap()))

            val get = batchGetItem(
                mapOf(table to ReqGetItem.Get(listOf(Item(attrS of "hello2"))))
            ).successValue()

            assertThat(get.UnprocessedKeys, equalTo(emptyMap()))
        }
    }

    @Test
    open fun `partiSQL operations`() {
        with(dynamo) {
            putItem(table, createItem("hello")).successValue()

            executeStatement(statement()).successValue()

            batchExecuteStatement(listOf(ReqStatement(statement()))).successValue()

//            executeTransaction(listOf(ParameterizedStatement(delete()))).successValue()
        }
    }

    @Test
    fun `item lifecycle`() {
        with(dynamo) {
            putItem(table, createItem("hello")).successValue()

            assertThat(getItem(table, Item(attrS of "hello4")).successValue().item, absent())

            val item = getItem(table, Item(attrS of "hello")).successValue().item!!

            assertThat(attrS(item), equalTo("hello"))
            assertThat(attrBool(item), equalTo(true))
            assertThat(attrB(item), equalTo(Base64Blob.encode("bar")))
            assertThat(attrBS(item), equalTo(setOf(Base64Blob.encode("bar"))))
            assertThat(attrN(item), equalTo(123))
            assertThat(attrNS(item), equalTo(setOf(123, 321)))
            assertThat(attrL(item), equalTo(listOf(List(listOf(Str("hello"))), Num(123), Null())))
            assertThat(attrSS(item), equalTo(setOf("345", "567")))
            assertThat(attrMissing(item), absent())
            assertThat(attrNL(item), absent())
            assertThat(attrM(item), equalTo(Item(attrS of "hello", attrBool of false)))

            updateItem(
                table,
                Item(attrS of "hello"),
                null,
                "set $attrN = :val1",
                ExpressionAttributeValues = mapOf(":val1" to attrN.asValue(321))
            ).successValue()

            val updatedItem = getItem(table, Item(attrS of "hello"), ConsistentRead = true).successValue().item!!
            assertThat(attrN(updatedItem), equalTo(321))

            val query = dynamo.query(
                table,
                KeyConditionExpression = "$attrS = :v1",
                ExpressionAttributeValues = mapOf(":v1" to attrS.asValue("hello")),
                ReturnConsumedCapacity = TOTAL
            ).successValue().items

            assertThat(attrN[query.first()], equalTo(321))

            val scan = dynamo.scan(
                table,
                ReturnConsumedCapacity = TOTAL
            ).successValue().items

            assertThat(attrN[scan.first()], equalTo(321))

            deleteItem(table, Item(attrS of "hello")).successValue()
        }
    }

    @Test
    fun `failed put condition returns the current item only when requested`() {
        with(dynamo) {
            val stored = createMiniItem("hello", bool = true)
            putItem(table, stored).successValue()

            // the attempted item differs from the stored one, so the assertions below distinguish
            // "returns the row which blocked the write" from "echoes back what was sent"
            val attempted = createMiniItem("hello", bool = false)

            val requested = putItem(
                table,
                attempted,
                ConditionExpression = "attribute_not_exists($attrS)",
                ReturnValuesOnConditionCheckFailure = ALL_OLD
            ).failureValue()

            assertThat(requested.status, equalTo(BAD_REQUEST))
            assertThat(requested.conditionCheckFailureItem(), equalTo(stored))

            val notRequested = putItem(
                table,
                attempted,
                ConditionExpression = "attribute_not_exists($attrS)"
            ).failureValue()

            assertThat(notRequested.status, equalTo(BAD_REQUEST))
            assertThat(notRequested.conditionCheckFailureItem(), absent())
        }
    }

    @Test
    fun `failed update condition returns the current item only when requested`() {
        with(dynamo) {
            val stored = createMiniItem("hello", bool = true)
            putItem(table, stored).successValue()

            // the stored item holds theBool = true, so requiring false is the failing condition
            val requested = updateItem(
                table,
                Item(attrS of "hello"),
                ConditionExpression = "$attrBool = :expected",
                UpdateExpression = "SET $attrN = :updated",
                ExpressionAttributeValues = mapOf(
                    ":expected" to attrBool.asValue(false),
                    ":updated" to attrN.asValue(99)
                ),
                ReturnValuesOnConditionCheckFailure = ALL_OLD
            ).failureValue()

            assertThat(requested.status, equalTo(BAD_REQUEST))
            assertThat(requested.conditionCheckFailureItem(), equalTo(stored))

            val notRequested = updateItem(
                table,
                Item(attrS of "hello"),
                ConditionExpression = "$attrBool = :expected",
                UpdateExpression = "SET $attrN = :updated",
                ExpressionAttributeValues = mapOf(
                    ":expected" to attrBool.asValue(false),
                    ":updated" to attrN.asValue(99)
                )
            ).failureValue()

            assertThat(notRequested.status, equalTo(BAD_REQUEST))
            assertThat(notRequested.conditionCheckFailureItem(), absent())

            // the failed condition must have left the stored record untouched
            assertThat(getItem(table, Item(attrS of "hello")).successValue().item, equalTo(stored))
        }
    }

    @Test
    fun `failed delete condition returns the current item only when requested`() {
        with(dynamo) {
            val stored = createMiniItem("hello", bool = true)
            putItem(table, stored).successValue()

            val requested = deleteItem(
                table,
                Item(attrS of "hello"),
                ConditionExpression = "$attrBool = :expected",
                ExpressionAttributeValues = mapOf(":expected" to attrBool.asValue(false)),
                ReturnValuesOnConditionCheckFailure = ALL_OLD
            ).failureValue()

            assertThat(requested.status, equalTo(BAD_REQUEST))
            assertThat(requested.conditionCheckFailureItem(), equalTo(stored))

            val notRequested = deleteItem(
                table,
                Item(attrS of "hello"),
                ConditionExpression = "$attrBool = :expected",
                ExpressionAttributeValues = mapOf(":expected" to attrBool.asValue(false))
            ).failureValue()

            assertThat(notRequested.status, equalTo(BAD_REQUEST))
            assertThat(notRequested.conditionCheckFailureItem(), absent())

            // a condition which cannot hold for a record that is not there fails with nothing to return
            val onMissing = deleteItem(
                table,
                Item(attrS of "ghost"),
                ConditionExpression = "attribute_exists($attrS)",
                ReturnValuesOnConditionCheckFailure = ALL_OLD
            ).failureValue()

            assertThat(onMissing.status, equalTo(BAD_REQUEST))
            assertThat(onMissing.conditionCheckFailureItem(), absent())
        }
    }

    @Test
    fun `conditional delete applies only when the condition holds`() {
        with(dynamo) {
            val stored = createMiniItem("hello", bool = true)
            putItem(table, stored).successValue()

            deleteItem(
                table,
                Item(attrS of "hello"),
                ConditionExpression = "$attrBool = :expected",
                ExpressionAttributeValues = mapOf(":expected" to attrBool.asValue(false))
            ).failureValue()

            assertThat(getItem(table, Item(attrS of "hello")).successValue().item, equalTo(stored))

            deleteItem(
                table,
                Item(attrS of "hello"),
                ConditionExpression = "$attrBool = :expected",
                ExpressionAttributeValues = mapOf(":expected" to attrBool.asValue(true))
            ).successValue()

            assertThat(getItem(table, Item(attrS of "hello")).successValue().item, absent())
        }
    }

    @Test
    fun `pagination of results`() {
        with(dynamo) {
            putItem(table, createItem("hello")).successValue()
            putItem(table, createItem("hello2")).successValue()
            putItem(table, createItem("hello3")).successValue()
            putItem(table, createItem("hello4")).successValue()
            putItem(table, createItem("hello5")).successValue()

            scanPaginated(table).forEach {
                assertThat(it.successValue().size, equalTo(5))
            }
            queryPaginated(
                table,
                KeyConditionExpression = "$attrS = :v1",
                ExpressionAttributeValues = mapOf(":v1" to attrS.asValue("hello"))
            ).forEach {
                assertThat(it.successValue().size, equalTo(1))
            }
            listTablesPaginated().forEach {
                assertThat(it.successValue().size, greaterThan(0))
            }
        }
    }

    @Test
    fun `table lifecycle`() {
        with(dynamo) {
            assertThat(listTables().successValue().TableNames, hasElement(table))

            assertThat(describeTable(table).successValue().Table.ItemCount, equalTo(0))

            assertThat(
                updateTable(
                    table,
                    BillingMode = PROVISIONED,
                    ProvisionedThroughput = ProvisionedThroughput(2, 1)
                ).successValue()
                    .TableDescription.TableName,
                equalTo(table)
            )

            waitForUpdate()
        }
    }

    @Test
    fun `time to live lifecycle`() {
        with(dynamo) {
            // A fresh table has TTL disabled.
            assertThat(
                describeTimeToLive(table).successValue().TimeToLiveDescription.TimeToLiveStatus,
                equalTo(DISABLED)
            )

            // Enabling echoes the requested specification back.
            val ttlAttribute = AttributeName.of("expiresAt")
            val specified = updateTimeToLive(
                table,
                TimeToLiveSpecification(Enabled = true, AttributeName = ttlAttribute)
            ).successValue().TimeToLiveSpecification
            assertThat(specified.Enabled, equalTo(true))
            assertThat(specified.AttributeName, equalTo(ttlAttribute))

            waitForUpdate()

            // Describe now names the attribute TTL applies to. (The status may be ENABLING before it
            // settles on ENABLED against real DynamoDB, so only the attribute is asserted here.) Disabling
            // is deliberately NOT exercised here: AWS rejects a second UpdateTimeToLive within the (up to
            // one hour) processing window with a ValidationException, so it belongs in a fake-only test,
            // not this contract that also runs against real DynamoDB (RealDynamoDbTest).
            assertThat(
                describeTimeToLive(table).successValue().TimeToLiveDescription.AttributeName,
                equalTo(ttlAttribute)
            )
        }
    }

    @Test
    fun `migrate data beetween tables`() {
        with(dynamo) {
            val destination = TableName.sample()
            try {
                val hashKey = attrS
                val rangeKey = attrN
                assertThat(
                    createTable(destination, hashKey = hashKey, rangeKey = rangeKey).TableDescription.ItemCount,
                    equalTo(0)
                )
                waitForUpdate()

                val expected = (0..10).map {
                    putItem(table, createMiniItem("hello$it", bool = true)).successValue()
                    createMiniItem("hello$it", bool = false)
                }

                copy(Scan(table, Limit = 25), destination) { it.with(attrBool of false) }

                assertThat(
                    scanPaginated(destination).toList().map { it.valueOrNull()!! }.flatten().toSet(),
                    equalTo(expected.toSet())
                )
            } finally {
                deleteTable(destination)
            }
        }
    }

    private fun delete() = """DELETE FROM "$table" WHERE "$attrS" = "hello";"""
    private fun statement() = """SELECT "$attrS" FROM "$table" WHERE "$attrS" = "hello";"""

    private fun waitForUpdate() = Thread.sleep(duration.toMillis())

    @Test
    @Disabled
    fun `delete tables`() {
        dynamo.listTables()
            .successValue()
            .TableNames
            .filter { it.value.startsWith("http4k-connect") }
            .forEach { dynamo.deleteTable(it) }
    }
}
