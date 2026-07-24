package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.DeleteTable
import org.http4k.connect.amazon.dynamodb.action.TableDescriptionResponse
import org.http4k.connect.amazon.dynamodb.model.TableStatus.DELETING
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveDescription
import org.http4k.connect.storage.Storage

fun AwsJsonFake.deleteTable(
    tables: Storage<DynamoTable>,
    timeToLive: Storage<TimeToLiveDescription>
) = route<DeleteTable> {
    tables[it.TableName.value]?.let { current ->
        val name = current.table.TableName!!.value
        tables.remove(name)
        // AWS drops a table's TTL config with the table, so one recreated under the same name starts
        // DISABLED — clear our separate TTL storage to match, rather than leaking the old config.
        timeToLive.remove(name)
        TableDescriptionResponse(current.table.copy(TableStatus = DELETING))
    }
}
