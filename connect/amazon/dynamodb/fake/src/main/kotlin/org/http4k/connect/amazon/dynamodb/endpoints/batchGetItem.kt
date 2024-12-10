package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.BatchGetItem
import org.http4k.connect.amazon.dynamodb.action.BatchGetItems
import org.http4k.connect.storage.Storage

fun AwsJsonFake.batchGetItem(tables: Storage<DynamoTable>) = route<BatchGetItem> { batchGetItem ->
    val items = batchGetItem.RequestItems.flatMap { (tableName, get) ->
        get.Keys.mapNotNull { key ->
            tables[tableName.value]
                ?.let { table ->
                    table.retrieve(key)
                        ?.project(get.ProjectionExpression, get.ExpressionAttributeNames)
                }
                ?.let { tableName.value to it }
        }
    }


    BatchGetItems(
        Responses = items
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } },
        UnprocessedKeys = emptyMap()
    )
}
