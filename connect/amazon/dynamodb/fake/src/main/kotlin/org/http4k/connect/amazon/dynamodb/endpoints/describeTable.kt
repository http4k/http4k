package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AmazonJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.DescribeTable
import org.http4k.connect.amazon.dynamodb.action.DescribedTable
import org.http4k.connect.storage.Storage

fun AmazonJsonFake.describeTable(tables: Storage<DynamoTable>) = route<DescribeTable> {
    tables[it.TableName.value]?.let { DescribedTable(it.table) }
}
