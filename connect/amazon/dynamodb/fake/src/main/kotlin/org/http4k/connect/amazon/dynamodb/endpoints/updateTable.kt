package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.TableDescriptionResponse
import org.http4k.connect.amazon.dynamodb.action.UpdateTable
import org.http4k.connect.storage.Storage

fun AwsJsonFake.updateTable(tables: Storage<DynamoTable>) = route<UpdateTable> { update ->
    tables[update.TableName.value]
        ?.let { current ->
            val updated = current.table.copy(
                AttributeDefinitions = listOfNotNull(
                    current.table.AttributeDefinitions,
                    update.AttributeDefinitions
                ).flatten()
            )
            tables[update.TableName.value] = current.copy(table = updated)

            TableDescriptionResponse(updated)
        }
}

