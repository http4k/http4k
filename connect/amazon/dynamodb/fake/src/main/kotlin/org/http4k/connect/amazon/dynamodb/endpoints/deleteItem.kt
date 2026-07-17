package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.DeleteItem
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.ConditionFailed
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.UpdateOk
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.storage.Storage

fun AwsJsonFake.deleteItem(tables: Storage<DynamoTable>) = route<DeleteItem>(
    responseFn = { conditionCheckAware(it) }
) { req ->
    tables.runUpdate(req.TableName, req, tryModifyDelete)
}

internal val tryModifyDelete = TryModifyItem<DeleteItem> { req, table ->
    val stored = table.retrieve(req.Key)
    if (req.ConditionExpression != null) {
        (stored ?: Item()).condition(
            expression = req.ConditionExpression,
            expressionAttributeNames = req.ExpressionAttributeNames,
            expressionAttributeValues = req.ExpressionAttributeValues
        )
            ?.let { removed(table, req.Key, stored) }
            ?: ConditionFailed(stored.returnedOnConditionFailure(req.ReturnValuesOnConditionCheckFailure))
    } else {
        removed(table, req.Key, stored)
    }
}

/** Deleting a record which is not there is not an error - DynamoDB reports it as a plain success. */
private fun removed(table: DynamoTable, key: Key, stored: Item?) = when (stored) {
    null -> UpdateOk(Item(), table)
    else -> UpdateOk(stored, table.withoutItem(key))
}
