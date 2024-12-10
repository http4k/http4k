package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.Scan
import org.http4k.connect.amazon.dynamodb.action.ScanResponse
import org.http4k.connect.amazon.dynamodb.grammar.DynamoDbConditionError
import org.http4k.connect.storage.Storage

fun AwsJsonFake.scan(tables: Storage<DynamoTable>) = route<Scan> { scan ->
    val table = tables[scan.TableName.value] ?: return@route null
    val schema = if (scan.IndexName != null) {
        table.table.keySchema(scan.IndexName) ?: return@route JsonError(
            "com.amazon.coral.validate#ValidationException",
            "The table does not have the specified index: ${scan.IndexName}"
        )
    } else table.table.KeySchema
    val comparator = schema.comparator(true)

    val matches = table.items
        .asSequence()
        .filter(schema.filterNullKeys())  // exclude items not held by selected index
        .sortedWith(comparator)  // sort by selected index
        .dropWhile {
            scan.ExclusiveStartKey != null && comparator.compare(
                it,
                scan.ExclusiveStartKey!!
            ) <= 0
        }   // skip previous pages
        .toList()

    val page = matches.take((scan.Limit ?: table.maxPageSize).coerceAtMost(table.maxPageSize))
    val filteredPage = try {
        page.mapNotNull {
            it.condition(
                expression = scan.FilterExpression,
                expressionAttributeNames = scan.ExpressionAttributeNames,
                expressionAttributeValues = scan.ExpressionAttributeValues
            )
        }
    } catch (e: DynamoDbConditionError) {
        return@route JsonError("com.amazon.coral.validate#ValidationException", "Invalid FilterExpression: ${e.message}")
    }

    ScanResponse(
        Count = filteredPage.size,
        Items = filteredPage.map { it.asItemResult() },
        LastEvaluatedKey = if (page.size < matches.size && schema != null) {
            val lastItem = page.lastOrNull()
            val indexKey = lastItem?.key(schema)
            val primaryKey = lastItem?.key(table.table.KeySchema!!)
            (indexKey.orEmpty() + primaryKey.orEmpty()).takeIf { it.isNotEmpty() }
        } else null
    )
}
