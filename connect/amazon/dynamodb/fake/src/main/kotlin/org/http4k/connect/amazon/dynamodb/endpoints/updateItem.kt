package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.UpdateItem
import org.http4k.connect.storage.Storage

fun AwsJsonFake.updateItem(tables: Storage<DynamoTable>) = route<UpdateItem> { req ->
    tables.runUpdate(req.TableName, req, tryModifyUpdate)
}

internal val tryModifyUpdate = TryModifyItem<UpdateItem> { req, table ->
    val existingItem = table.retrieve(req.Key) ?: req.Key
    if (req.ConditionExpression != null) {
        existingItem.condition(
            expression = req.ConditionExpression,
            expressionAttributeNames = req.ExpressionAttributeNames,
            expressionAttributeValues = req.ExpressionAttributeValues
        )
            ?.let {
                val updated = existingItem.update(
                    expression = req.UpdateExpression,
                    expressionAttributeNames = req.ExpressionAttributeNames,
                    expressionAttributeValues = req.ExpressionAttributeValues
                )

                UpdateResult.UpdateOk(updated, table.withoutItem(existingItem).withItem(updated))
            }
            ?: UpdateResult.ConditionFailed
    } else {

        val updated = existingItem.update(
            expression = req.UpdateExpression,
            expressionAttributeNames = req.ExpressionAttributeNames,
            expressionAttributeValues = req.ExpressionAttributeValues
        )

        UpdateResult.UpdateOk(updated, table.withoutItem(existingItem).withItem(updated))
    }
}
