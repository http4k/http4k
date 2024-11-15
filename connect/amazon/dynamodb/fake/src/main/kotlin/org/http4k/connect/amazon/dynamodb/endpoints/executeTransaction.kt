package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AmazonJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.ExecuteTransaction
import org.http4k.connect.storage.Storage

fun AmazonJsonFake.executeTransaction(tables: Storage<DynamoTable>) = route<ExecuteTransaction> {
    synchronized(tables) {
        null
    }
}
