package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.PutItem
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.ConditionFailed
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.UpdateOk
import org.http4k.connect.storage.Storage

fun AwsJsonFake.putItem(tables: Storage<DynamoTable>) = route<PutItem> { req ->
    tables.runUpdate(req.TableName, req, tryModifyPut)
}

internal val tryModifyPut = TryModifyItem<PutItem> { req, table ->
    if (req.ConditionExpression != null) {
        val existingItem = table.retrieve(req.Item.key(table.table.KeySchema!!))
        if (existingItem != null) {
            existingItem.condition(
                expression = req.ConditionExpression,
                expressionAttributeNames = req.ExpressionAttributeNames,
                expressionAttributeValues = req.ExpressionAttributeValues
            )
                ?.let { UpdateOk(req.Item, table.withItem(req.Item)) }
                ?: ConditionFailed
        } else {
            UpdateOk(req.Item, table.withItem(req.Item))
        }
    } else {
        UpdateOk(req.Item, table.withItem(req.Item))
    }
}
