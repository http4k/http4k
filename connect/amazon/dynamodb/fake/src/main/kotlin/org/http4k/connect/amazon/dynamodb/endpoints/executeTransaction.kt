package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.ExecuteTransaction
import org.http4k.connect.storage.Storage

fun AwsJsonFake.executeTransaction(tables: Storage<DynamoTable>) = route<ExecuteTransaction> {
    synchronized(tables) {
        null
    }
}
