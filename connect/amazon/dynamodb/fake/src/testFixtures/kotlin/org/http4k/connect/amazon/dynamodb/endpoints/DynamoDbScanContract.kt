package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.lessThan
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Failure
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource
import org.http4k.connect.amazon.dynamodb.LocalDynamoDbSource
import org.http4k.connect.amazon.dynamodb.attrB
import org.http4k.connect.amazon.dynamodb.attrBool
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.attrSS
import org.http4k.connect.amazon.dynamodb.batchWriteItem
import org.http4k.connect.amazon.dynamodb.createItem
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.Projection
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.connect.amazon.dynamodb.model.without
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.scan
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

abstract class DynamoDbScanContract : DynamoDbSource {

    private val table = TableName.sample()
    private val item1 = createItem("hash1", 1, Base64Blob.encode("foo"))
    private val item2 = createItem("hash2", 1, Base64Blob.encode("bar"))
    private val item3 = createItem("hash3", 2).without(attrB)

    private val binaryStringGSI = IndexName.of("bin-string")
    private val attrNGsi = IndexName.of(attrN.name.value)

    @BeforeEach
    fun createTable() {
        dynamo.createTable(
            table,
            KeySchema = KeySchema.compound(attrS.name),
            AttributeDefinitions = listOf(
                attrS.asAttributeDefinition(),
                attrB.asAttributeDefinition(),
                attrN.asAttributeDefinition()
            ),
            GlobalSecondaryIndexes = listOf(
                GlobalSecondaryIndex(
                    IndexName = binaryStringGSI,
                    KeySchema.compound(attrB.name, attrS.name),
                    Projection.all
                ),
                GlobalSecondaryIndex(IndexName = attrNGsi, KeySchema.compound(attrN.name), Projection.all),
            ),
            BillingMode = BillingMode.PAY_PER_REQUEST
        ).successValue()

        dynamo.waitForExist(table)

        dynamo.putItem(table, item1).successValue()
        dynamo.putItem(table, item2).successValue()
        dynamo.putItem(table, item3).successValue()
    }

    @Test
    fun `scan table`() {
        val result = dynamo.scan(table).successValue()

        assertThat(result.Count, equalTo(3))
        assertThat(result.items, hasSize(equalTo(3)))
        assertThat(result.items, hasElement(item1))
        assertThat(result.items, hasElement(item2))
        assertThat(result.items, hasElement(item3))
        assertThat(result.LastEvaluatedKey, absent())
    }

    @Test
    fun `scan with filter`() {
        val result = dynamo.scan(
            TableName = table,
            FilterExpression = "$attrN = :val1",
            ExpressionAttributeValues = mapOf(":val1" to attrN.asValue(1))
        ).successValue()

        assertThat(result.Count, equalTo(2))
        assertThat(result.items, hasSize(equalTo(2)))
        assertThat(result.items, hasElement(item1))
        assertThat(result.items, hasElement(item2))
        assertThat(result.LastEvaluatedKey, absent())
    }

    @Test
    fun `scan with limit`() {
        val result = dynamo.scan(TableName = table, Limit = 2).successValue()

        assertThat(result.Count, equalTo(2))
        assertThat(result.items, hasSize(equalTo(2)))
        assertThat(result.LastEvaluatedKey, present())
    }

    @Test
    fun `scan multiple pages`() {
        val page1 = dynamo.scan(
            TableName = table,
            Limit = 2
        ).successValue()

        assertThat(page1.Count, equalTo(2))
        assertThat(page1.items, hasSize(equalTo(2)))
        assertThat(page1.LastEvaluatedKey, present())

        val page2 = dynamo.scan(
            TableName = table,
            ExclusiveStartKey = page1.LastEvaluatedKey
        ).successValue()

        assertThat(page2.Count, equalTo(1))
        page2.items.forEach { assertThat(page1.items, hasElement(it).not()) }
        assertThat(page2.LastEvaluatedKey, absent())
    }

    @Test
    fun `scan with max results for page`() {
        val numItems = 2_000
        val payload = (1..1_000).map { "a".repeat(1_000) }.toSet()

        (1..numItems).chunked(25).forEach { chunk ->
            dynamo.batchWriteItem(
                mapOf(
                    table to chunk.map { index ->
                        ReqWriteItem.Put(Item(attrS of "hash$index", attrSS of payload))
                    }
                )
            ).successValue()
        }

        val result = dynamo.scan(
            TableName = table
        ).successValue()

        assertThat(result.Count, present(lessThan(numItems)))
        assertThat(result.LastEvaluatedKey, present())
    }

    @Test
    fun `scan index`() {
        val result = dynamo.scan(TableName = table, IndexName = binaryStringGSI).successValue()

        assertThat(result.Count, equalTo(2))
        assertThat(result.items, hasSize(equalTo(2)))
        assertThat(result.items, hasElement(item1))
        assertThat(result.items, hasElement(item2))
        assertThat(result.LastEvaluatedKey, absent())
    }

    @Test // Fixes GH#327
    fun `filter evaluated after pagination`() {
        dynamo.batchWriteItem(
            mapOf(
                table to listOf(
                    ReqWriteItem.Put(Item(attrS of "hash1", attrN of 1, attrBool of true)),
                    ReqWriteItem.Put(Item(attrS of "hash2", attrN of 2, attrBool of true)),
                    ReqWriteItem.Put(Item(attrS of "hash3", attrN of 3, attrBool of false)),
                    ReqWriteItem.Put(Item(attrS of "hash4", attrN of 4, attrBool of false)),
                    ReqWriteItem.Put(Item(attrS of "hash5", attrN of 5, attrBool of false))
                )
            )
        ).successValue()

        val result = dynamo.scan(
            TableName = table,
            IndexName = attrNGsi,
            FilterExpression = "$attrBool = :val1",
            ExpressionAttributeValues = mapOf(
                ":val1" to attrBool.asValue(true)
            ),
            Limit = 4,
        ).successValue()

        assertThat(result.Count, present(equalTo(2)))
        assertThat(
            result.items, equalTo(
                listOf(
                    Item(attrS of "hash1", attrN of 1, attrBool of true),
                    Item(attrS of "hash2", attrN of 2, attrBool of true)
                )
            )
        )
        assertThat(result.LastEvaluatedKey, equalTo(Item(attrS of "hash4", attrN of 4)))
    }

    @Test
    fun `paginate on GSI - different keys than primary index`() {
        val idAttr = Attribute.uuid().required("id")
        val nameAttr = Attribute.string().required("name")
        val dobAttr = Attribute.localDate().required("dob")

        val searchIndex = IndexName.of("search")
        val table = TableName.of("people")

        dynamo.createTable(
            table,
            KeySchema = KeySchema.compound(idAttr.name),
            AttributeDefinitions = listOf(
                idAttr.asAttributeDefinition(),
                nameAttr.asAttributeDefinition(),
                dobAttr.asAttributeDefinition()
            ),
            GlobalSecondaryIndexes = listOf(
                GlobalSecondaryIndex(
                    IndexName = searchIndex,
                    KeySchema.compound(dobAttr.name, nameAttr.name),
                    Projection.all
                )
            ),
            BillingMode = BillingMode.PAY_PER_REQUEST
        ).successValue()

        dynamo.waitForExist(table)

        val dob1 = LocalDate.of(2024, 2, 29)

        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()

        val item1 = Item(idAttr of id1, dobAttr of dob1, nameAttr of "name1").also { dynamo.putItem(table,it) }
        val item2 = Item(idAttr of id2, dobAttr of dob1, nameAttr of "name2").also { dynamo.putItem(table,it) }
        val item3 = Item(idAttr of id3, dobAttr of dob1, nameAttr of "name3").also { dynamo.putItem(table,it) }

        val page1 = dynamo.scan(
            TableName = table,
            IndexName = searchIndex,
            Limit = 2
        ).successValue()

        assertThat(page1.items, equalTo(listOf(item1, item2)))
        assertThat(page1.LastEvaluatedKey, equalTo(
            Key(
                idAttr of id2,
                nameAttr of "name2",
                dobAttr of dob1
            )
        ))

        val page2 = dynamo.scan(
            TableName = table,
            IndexName = searchIndex,
            Limit = 2,
            ExclusiveStartKey = page1.LastEvaluatedKey
        ).successValue()

        assertThat(page2.items, equalTo(listOf(item3)))
        assertThat(page2.LastEvaluatedKey, absent())
    }

    @Test
    fun `scan on missing index`() {
        val result = dynamo.scan(
            TableName = table,
            IndexName = IndexName.of("missing")
        )
        assertThat(result, equalTo(
            Failure(RemoteFailure(
                method = Method.POST,
                uri = Uri.of("/"),
                status = Status.BAD_REQUEST,
                message = """{"__type":"com.amazon.coral.validate#ValidationException","Message":"The table does not have the specified index: missing"}"""
            ))
        ))
    }

    @Test
    fun `scan with reserved word - exact case`() {
        dynamo.putItem(table, item1)

        val result = dynamo.scan(
            TableName = table,
            FilterExpression = "ARRAY = :val1",
            ExpressionAttributeValues = mapOf(
                ":val1" to attrN.asValue(1)
            )
        )
        assertThat(result, equalTo(Failure(RemoteFailure(
            method = Method.POST,
            uri = Uri.of("/"),
            status = Status.BAD_REQUEST,
            message = """{"__type":"com.amazon.coral.validate#ValidationException","Message":"Invalid FilterExpression: Attribute name is a reserved keyword; reserved keyword: ARRAY"}"""
        ))))
    }

    @Test
    fun `scan with reserved word - named`() {
        dynamo.putItem(table, item1)

        dynamo.scan(
            TableName = table,
            FilterExpression = "#key1 = :val1",
            ExpressionAttributeNames = mapOf(
                "#key1" to AttributeName.of("ARRAY")
            ),
            ExpressionAttributeValues = mapOf(
                ":val1" to attrN.asValue(1)
            )
        ).successValue()
    }

    @Test
    fun `query with reserved word - ignore case`() {
        dynamo.putItem(table, item1)

        val result = dynamo.scan(
            TableName = table,
            FilterExpression = "aRrAy = :val1",
            ExpressionAttributeValues = mapOf(
                ":val1" to attrN.asValue(1)
            )
        )
        assertThat(result, equalTo(Failure(RemoteFailure(
            method = Method.POST,
            uri = Uri.of("/"),
            status = Status.BAD_REQUEST,
            message = """{"__type":"com.amazon.coral.validate#ValidationException","Message":"Invalid FilterExpression: Attribute name is a reserved keyword; reserved keyword: aRrAy"}"""
        ))))
    }
}

class LocalDynamoDbScanTest : DynamoDbScanContract(), DynamoDbSource by LocalDynamoDbSource()
class FakeDynamoDbScanTest : DynamoDbScanContract(), DynamoDbSource by FakeDynamoDbSource()
