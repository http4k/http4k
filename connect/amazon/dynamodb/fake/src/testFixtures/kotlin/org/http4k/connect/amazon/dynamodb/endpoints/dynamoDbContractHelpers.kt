package org.http4k.connect.amazon.dynamodb.endpoints

import dev.forkhandles.result4k.Success
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.action.DescribedTable
import org.http4k.connect.amazon.dynamodb.describeTable
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.waitUntil
import java.time.Duration

fun DynamoDb.waitForExist(name: TableName, timeout: Duration = Duration.ofSeconds(10)) {
    waitUntil(
        { describeTable(name) is Success<DescribedTable> },
        failureMessage = "Table $name was not created after $timeout",
        timeout = timeout
    )
}
