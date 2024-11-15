package org.http4k.connect.amazon.dynamodb.action

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.connect.Paged
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.batchWriteItem
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.paginated

/**
 * Copies items that are queried into another table, using BatchWriteItem to insert, and optionally mapping.
 * Returns the Unprocessed Items from the last insert if there were any.
 */
fun <R : Paged<Key, Item>, Self : DynamoDbPagedAction<R, Self>> DynamoDb.copy(
    action: Self, destination: TableName, mappingFn: (Item) -> Item = { it }
): Result<Map<String, ReqWriteItem>?, RemoteFailure> {
    require(action.Limit != null && action.Limit!! > 0 && action.Limit!! <= 25) { "Limit must be <=25" }
    return paginated(::invoke, action)
        .map { it.flatMap { batchWriteItem(mapOf(destination to it.map(mappingFn).map { ReqWriteItem.Put(it) })) } }
        .takeWhile {
            val succeeded = it.valueOrNull() != null
            val hasUnprocessedItems = it.valueOrNull()?.UnprocessedItems?.takeIf { it.isNotEmpty() } != null
            succeeded && !hasUnprocessedItems
        }
        .lastOrNull()
        ?.map { it.UnprocessedItems }
        ?: Success(null)
}
