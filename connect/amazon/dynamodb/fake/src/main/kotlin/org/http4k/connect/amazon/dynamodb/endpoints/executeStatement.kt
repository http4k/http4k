package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AmazonJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.ExecuteStatement
import org.http4k.connect.storage.Storage

fun AmazonJsonFake.executeStatement(tables: Storage<DynamoTable>) = route<ExecuteStatement> {
    null
}
