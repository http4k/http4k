package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource
import org.http4k.connect.amazon.dynamodb.LocalDynamoDbSource
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.batchWriteItem
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.scan
import org.http4k.connect.successValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class DynamoDbBatchWriteItemTest : DynamoDbSource {

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
    fun `batch put multiple items`() {
        val item1 = Item(attrS of "item1")
        val item2 = Item(attrS of "item2")
        val item3 = Item(attrS of "item3")

        dynamo.batchWriteItem(
            mapOf(
                table to listOf(
                    ReqWriteItem.Put(item1),
                    ReqWriteItem.Put(item2),
                    ReqWriteItem.Put(item3)
                )
            )
        ).successValue()

        val items = dynamo.scan(table).successValue().items

        assertThat(items, hasSize(equalTo(3)))
        assertThat(items.toSet(), equalTo(setOf(item1, item2, item3)))
    }
}

class FakeDynamoDbBatchWriteItemTest : DynamoDbBatchWriteItemTest(), DynamoDbSource by FakeDynamoDbSource()
class LocalDynamoDbBatchWriteItemTest : DynamoDbBatchWriteItemTest(), DynamoDbSource by LocalDynamoDbSource()
