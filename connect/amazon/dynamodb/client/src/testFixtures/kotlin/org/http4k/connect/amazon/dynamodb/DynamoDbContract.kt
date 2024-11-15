package org.http4k.connect.amazon.dynamodb

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.hasElement
import dev.forkhandles.result4k.valueOrNull
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.dynamodb.action.Scan
import org.http4k.connect.amazon.dynamodb.action.copy
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
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TransactGetItem.Companion.Get
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Delete
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Put
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem.Companion.Update
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class MyValueType(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<MyValueType>(::MyValueType)
}

interface DynamoDbContract : AwsContract {
    val duration: Duration

    private val dynamo get() = DynamoDb.Http(aws.region, { aws.credentials }, http)

    open val table get() = TableName.sample(suffix = uuid(0).toString())

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
