package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.DeleteItem
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.UpdateOk
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.storage.Storage

fun AwsJsonFake.deleteItem(tables: Storage<DynamoTable>) = route<DeleteItem> { req ->
    tables.runUpdate(req.TableName, req, tryModifyDelete)
}

internal val tryModifyDelete = TryModifyItem<DeleteItem> { req, table ->
    val existingItem = table.retrieve(req.Key)
    when {
        existingItem != null -> UpdateOk(existingItem, table.withoutItem(req.Key))
        else -> UpdateOk(Item(), table)
    }
}
