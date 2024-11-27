package org.http4k.connect.amazon.dynamodb

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.KeyType.HASH
import org.http4k.connect.amazon.dynamodb.model.TableDescription
import org.http4k.connect.amazon.dynamodb.model.asKeySchema
import org.junit.jupiter.api.Test

class DynamoTableTest {
    private val attS = Attribute.string().required("string")
    private val intS = Attribute.int().required("int")

    @Test
    fun `item lifecycle`() {
        val table = DynamoTable(TableDescription(KeySchema = listOf(attS.asKeySchema(HASH))))

        val stringKey = Item(attS of "hello")
        val item = stringKey + Item(intS of 123)
        val otherItem = stringKey + Item(intS of 345)

        assertThat(table.retrieve(stringKey), absent())

        val updated = table.withItem(item)
        assertThat(updated.retrieve(stringKey), equalTo(item))
        assertThat(updated.retrieve(otherItem), absent())

        val notDeleted = updated.withoutItem(otherItem)
        assertThat(notDeleted.items.toString(), notDeleted.retrieve(stringKey), equalTo(item))

        val deleted = notDeleted.withoutItem(stringKey)
        assertThat(deleted.retrieve(stringKey), absent())
    }
}
