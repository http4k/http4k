package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.Query
import org.http4k.connect.amazon.dynamodb.action.QueryResponse
import org.http4k.connect.amazon.dynamodb.grammar.DynamoDbConditionError
import org.http4k.connect.storage.Storage

fun AwsJsonFake.query(tables: Storage<DynamoTable>) = route<Query> { query ->
    val table = tables[query.TableName.value] ?: return@route null
    val schema = if (query.IndexName != null) {
        table.table.keySchema(query.IndexName) ?: return@route JsonError(
            "com.amazon.coral.validate#ValidationException",
            "The table does not have the specified index: ${query.IndexName}"
        )
    } else table.table.KeySchema

    val comparator = schema.comparator(query.ScanIndexForward ?: true)

    val matches = try {
        table.items
            .asSequence()
            .filter(schema.filterNullKeys()) // exclude items not held by selected index
            .mapNotNull {
                it.condition(
                    expression = query.KeyConditionExpression,
                    expressionAttributeNames = query.ExpressionAttributeNames,
                    expressionAttributeValues = query.ExpressionAttributeValues
                )
            }
            .sortedWith(comparator)  // sort by selected index
            .dropWhile {
                query.ExclusiveStartKey != null && comparator.compare(
                    it,
                    query.ExclusiveStartKey!!
                ) <= 0
            }  // skip previous pages
            .toList()
    } catch (e: DynamoDbConditionError) {
        return@route JsonError("com.amazon.coral.validate#ValidationException", "Invalid KeyConditionExpression: ${e.message}")
    }

    val page = matches.take((query.Limit ?: table.maxPageSize).coerceAtMost(table.maxPageSize))
    val filteredPage = try {
        page.mapNotNull {
            it.condition(
                expression = query.FilterExpression,
                expressionAttributeNames = query.ExpressionAttributeNames,
                expressionAttributeValues = query.ExpressionAttributeValues
            )
        }
    } catch (e: DynamoDbConditionError) {
        return@route JsonError("com.amazon.coral.validate#ValidationException", "Invalid FilterExpression: ${e.message}")
    }

    QueryResponse(
        Count = filteredPage.size,
        Items = filteredPage.map { it.asItemResult() },
        LastEvaluatedKey = page.lastOrNull()
            ?.takeIf { page.size < matches.size }
            ?.let { last ->
                buildMap {
                    this += last.key(table.table.KeySchema!!)
                    if (schema != null) {
                        this += last.key(schema)
                    }
                }
            }
    )
}
