package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.BatchWriteItem
import org.http4k.connect.amazon.dynamodb.action.BatchWriteItems
import org.http4k.connect.storage.Storage

fun AwsJsonFake.batchWriteItem(tables: Storage<DynamoTable>) = route<BatchWriteItem> {
    it.RequestItems
        .forEach { (tableName, writeItems) ->
            tables[tableName.value]
                ?.let { table ->
                    tables[tableName.value] = writeItems.fold(table) { curTable, req ->
                        when {
                            req.PutRequest != null -> curTable.withItem(req.PutRequest!!["Item"]!!)
                            req.DeleteRequest != null -> curTable.withoutItem(req.DeleteRequest!!["Key"]!!)
                            else -> curTable
                        }
                    }
                }
        }
    BatchWriteItems(UnprocessedItems = emptyMap())
}
