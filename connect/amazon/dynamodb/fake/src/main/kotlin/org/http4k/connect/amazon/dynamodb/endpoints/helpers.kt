@file:Suppress("UNCHECKED_CAST")

package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.ModifiedItem
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.NotFound
import org.http4k.connect.amazon.dynamodb.grammar.AttributeNameValue
import org.http4k.connect.amazon.dynamodb.grammar.DynamoDbConditionalGrammar
import org.http4k.connect.amazon.dynamodb.grammar.DynamoDbProjectionGrammar
import org.http4k.connect.amazon.dynamodb.grammar.DynamoDbUpdateGrammar
import org.http4k.connect.amazon.dynamodb.grammar.ItemWithSubstitutions
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.KeyType
import org.http4k.connect.amazon.dynamodb.model.TableDescription
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues
import org.http4k.connect.storage.Storage

fun Item.asItemResult(): Map<String, Map<String, Any>> =
    mapKeys { it.key.value }.mapValues { convert(it.value) }

inline fun <reified OUT : Any> convert(input: Any) = DynamoDbMoshi.asA<OUT>(DynamoDbMoshi.asFormatString(input))

/**
 * Transform the input item by applying the projection to the fields in it.
 */
fun Item.project(
    projectionExpression: String?,
    expressionAttributeNames: TokensToNames?
): Item = projectionExpression?.let {
    val item = ItemWithSubstitutions(this, expressionAttributeNames ?: emptyMap())
    val allItems: List<AttributeNameValue> = it.split(',')
        .map(String::trim)
        .map(DynamoDbProjectionGrammar::parse)
        .flatMap { it.eval(item) as List<AttributeNameValue> }

    allItems
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }
        .map { (name: AttributeName, values: List<AttributeValue>) ->
            name to when {
                values[0].L != null -> AttributeValue.List(values.flatMap { it.L!! })
                values[0].M != null -> AttributeValue.Map(values
                    .map { it.M!! }
                    .fold(Item()) { acc, next -> acc + next })

                else -> values[0]
            }
        }.toMap()
} ?: this

/**
 * Apply the conditional expression to the Item. If the condition is null or resolves to true returns the item,
 * or returns null.
 */
fun Item.condition(
    expression: String?,
    expressionAttributeNames: TokensToNames?,
    expressionAttributeValues: TokensToValues?
) = when (expression) {
    null -> this
    else -> takeIf {
        DynamoDbConditionalGrammar.parse(expression).eval(
            ItemWithSubstitutions(
                this,
                expressionAttributeNames ?: emptyMap(),
                expressionAttributeValues ?: emptyMap()
            )
        ) == true
    }
}

fun Item.update(
    expression: String?,
    expressionAttributeNames: TokensToNames?,
    expressionAttributeValues: TokensToValues?
) = when (expression) {
    null -> this
    else -> DynamoDbUpdateGrammar.parse(expression).eval(
        ItemWithSubstitutions(
            this,
            expressionAttributeNames ?: emptyMap(),
            expressionAttributeValues ?: emptyMap()
        )
    ) as Item
}

fun List<KeySchema>?.comparator(ascending: Boolean): Comparator<Item> {
    if (this == null) return Comparator { _, _ -> 0 }

    val hashKey = find { it.KeyType == KeyType.HASH }
        ?.AttributeName
        ?: return Comparator { _, _ -> 0 }

    val sortKey = find { it.KeyType == KeyType.RANGE }
        ?.AttributeName
    val modifier = if (ascending) 1 else -1

    return Comparator { item1: Item, item2: Item ->
        val hashValue1 = item1[hashKey] ?: return@Comparator 0
        val hashValue2 = item2[hashKey] ?: return@Comparator 0

        val hashComparison = hashValue1.compareTo(hashValue2) * modifier
        if (hashComparison != 0) return@Comparator hashComparison

        val sortValue1 = item1[sortKey] ?: return@Comparator 0
        val sortValue2 = item2[sortKey] ?: return@Comparator 0

        sortValue1.compareTo(sortValue2) * modifier
    }
}

fun List<KeySchema>?.filterNullKeys(): (Item) -> Boolean {
    val hashKey = this?.find { it.KeyType == KeyType.HASH }
        ?.AttributeName

    val sortKey = this?.find { it.KeyType == KeyType.RANGE }
        ?.AttributeName

    return { item: Item ->
        (hashKey == null || item[hashKey] != null) && (sortKey == null || item[sortKey] != null)
    }
}

fun TableDescription.keySchema(indexName: IndexName? = null): List<KeySchema>? {
    if (indexName == null) return KeySchema

    for (index in GlobalSecondaryIndexes ?: emptyList()) {
        if (index.IndexName == indexName.value) {
            return index.KeySchema
        }
    }

    return LocalSecondaryIndexes
        ?.find { it.IndexName == indexName.value }
        ?.KeySchema
}

fun Item.key(schema: List<KeySchema>): Key {
    return schema.mapNotNull { key ->
        val value = this[key.AttributeName]
        if (value == null) null else key.AttributeName to value
    }.toMap()
}

/**
 * The result of an update operation
 */
sealed interface UpdateResult {
    val result: Any?

    class UpdateOk(item: Item, val updatedTable: DynamoTable) : UpdateResult {
        override val result = ModifiedItem(item.asItemResult())
    }

    data object ConditionFailed : UpdateResult {
        override val result = JsonError(
            "com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException",
            "The conditional request failed"
        )
    }

    data object NotFound : UpdateResult {
        override val result = null
    }
}

internal fun <Req> Storage<DynamoTable>.runUpdate(table: TableName, t: Req, update: TryModifyItem<Req>): Any? {
    val updateResult = this[table.value]?.let { update(t, it) } ?: NotFound
    if (updateResult is UpdateResult.UpdateOk) this[table.value] = updateResult.updatedTable
    return updateResult.result
}

fun interface TryModifyItem<T> : (T, DynamoTable) -> UpdateResult
