package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi.convert
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.GetItem
import org.http4k.connect.amazon.dynamodb.action.GetItemsResponse
import org.http4k.connect.amazon.dynamodb.action.TransactGetItems
import org.http4k.connect.amazon.dynamodb.model.GetItemsResponseItem
import org.http4k.connect.storage.Storage

fun AwsJsonFake.transactGetItems(tables: Storage<DynamoTable>) = route<TransactGetItems> {
    synchronized(tables) {
        GetItemsResponse(
            it.TransactItems
                .map { convert<Map<String, Any?>, GetItem>(it.Get) }
                .map { get ->
                    GetItemsResponseItem(
                        tables[get.TableName.value]
                            ?.let { it.retrieve(get.Key)?.asItemResult() }
                    )
                }
        )
    }
}
