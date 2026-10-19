package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.GetItem
import org.http4k.connect.amazon.dynamodb.action.GetResponse
import org.http4k.connect.storage.Storage

fun AwsJsonFake.getItem(tables: Storage<DynamoTable>) = route<GetItem> { getItem ->
    unusedSubstitutions(getItem.ExpressionAttributeNames, null, getItem.ProjectionExpression)
        ?.let { return@route it }

    tables[getItem.TableName.value]
        ?.let { table ->
            val item =
                table.retrieve(getItem.Key)
                    ?.project(getItem.ProjectionExpression, getItem.ExpressionAttributeNames)
                    ?.asItemResult()
            GetResponse(item)
        }
}
