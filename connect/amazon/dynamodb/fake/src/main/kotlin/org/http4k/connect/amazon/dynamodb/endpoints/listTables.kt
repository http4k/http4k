package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.ListTables
import org.http4k.connect.amazon.dynamodb.action.TableList
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.storage.Storage

fun AwsJsonFake.listTables(tables: Storage<DynamoTable>) = route<ListTables> {
    TableList(tables.keySet("").map(TableName::of))
}
