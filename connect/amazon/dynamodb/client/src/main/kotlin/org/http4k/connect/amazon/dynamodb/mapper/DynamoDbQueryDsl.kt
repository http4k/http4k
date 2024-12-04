package org.http4k.connect.amazon.dynamodb.mapper

import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues

interface KeyCondition<HashKey : Any, SortKey : Any> {
    val expression: String
    val attributeNames: TokensToNames
    val attributeValues: TokensToValues
}

interface SortKeyCondition<HashKey : Any, SortKey : Any> : KeyCondition<HashKey, SortKey>
interface CombinedKeyCondition<HashKey : Any, SortKey : Any> : KeyCondition<HashKey, SortKey>
interface PartitionKeyCondition<HashKey : Any, SortKey : Any> : CombinedKeyCondition<HashKey, SortKey>

/**
 * See https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.KeyConditionExpressions.html
 */
class KeyConditionBuilder<HashKey : Any, SortKey : Any> internal constructor(
    schema: DynamoDbTableMapperSchema<*, HashKey, SortKey>
) {
    inner class HashKeySubstitute(val attribute: Attribute<HashKey>) {
        val expressionAttributeName = "hk"
    }

    inner class SortKeySubstitute(val attribute: Attribute<SortKey>?) {
        val expressionAttributeName = "sk"
    }

    val hashKey = HashKeySubstitute(schema.hashKeyAttribute)
    val sortKey = SortKeySubstitute(schema.sortKeyAttribute)

    infix fun HashKeySubstitute.eq(value: HashKey) = object : PartitionKeyCondition<HashKey, SortKey> {
        override val expression = "#$expressionAttributeName = :$expressionAttributeName"
        override val attributeNames = mapOf("#$expressionAttributeName" to attribute.name)
        override val attributeValues = mapOf(":$expressionAttributeName" to attribute.asValue(value))
    }

    private fun sortKeyCondition(
        expr: String,
        attrNames: TokensToNames,
        attrValues: TokensToValues
    ) = object : SortKeyCondition<HashKey, SortKey> {
        override val expression = expr
        override val attributeNames = attrNames
        override val attributeValues = attrValues
    }

    private fun Attribute<SortKey>.sortKeyOperator(op: String, value: SortKey) =
        SortKeySubstitute(this).sortKeyOperator(op, value)

    private fun SortKeySubstitute.sortKeyOperator(op: String, value: SortKey): SortKeyCondition<HashKey, SortKey>? =
        attribute?.let {
            sortKeyCondition(
                "#$expressionAttributeName $op :$expressionAttributeName",
                mapOf("#$expressionAttributeName" to attribute.name),
                mapOf(":$expressionAttributeName" to attribute.asValue(value))
            )
        }

    infix fun Attribute<SortKey>.ge(value: SortKey) = sortKeyOperator(">=", value)
    infix fun SortKeySubstitute.eq(value: SortKey) = sortKeyOperator("=", value)
    infix fun SortKeySubstitute.lt(value: SortKey) = sortKeyOperator("<", value)
    infix fun SortKeySubstitute.le(value: SortKey) = sortKeyOperator("<=", value)
    infix fun SortKeySubstitute.gt(value: SortKey) = sortKeyOperator(">", value)
    infix fun SortKeySubstitute.ge(value: SortKey) = sortKeyOperator(">=", value)

    fun SortKeySubstitute.between(value1: SortKey, value2: SortKey): SortKeyCondition<HashKey, SortKey>? =
        attribute?.let {
            sortKeyCondition(
                "#$expressionAttributeName BETWEEN :${expressionAttributeName}1 AND :${expressionAttributeName}2",
                mapOf("#$expressionAttributeName" to attribute.name),
                mapOf(
                    ":${expressionAttributeName}1" to attribute.asValue(value1),
                    ":${expressionAttributeName}2" to attribute.asValue(value2)
                )
            )
        }

    infix fun SortKeySubstitute.beginsWith(value: SortKey): SortKeyCondition<HashKey, SortKey>? =
        attribute?.let {
            sortKeyCondition(
                "begins_with(#$expressionAttributeName,:$expressionAttributeName)",
                mapOf("#$expressionAttributeName" to attribute.name),
                mapOf(":$expressionAttributeName" to attribute.asValue(value))
            )
        }

    infix fun PartitionKeyCondition<HashKey, SortKey>.and(secondary: SortKeyCondition<HashKey, SortKey>?): CombinedKeyCondition<HashKey, SortKey> =
        let {
            if (secondary == null) {
                this
            } else {
                object : CombinedKeyCondition<HashKey, SortKey> {
                    override val expression = "${it.expression} AND ${secondary.expression}"
                    override val attributeNames = it.attributeNames + secondary.attributeNames
                    override val attributeValues = it.attributeValues + secondary.attributeValues
                }
            }
        }
}

class FilterExpression(
    val expression: String,
    val attributeNames: TokensToNames,
    val attributeValues: TokensToValues,
)

/**
 * See https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Syntax
 */
class FilterExpressionBuilder internal constructor() {

    private var attributeNameCount = 0

    // generate consecutive names "fa", "fb", ... to be used as expression attribute names
    private fun nextAttributeName(): String {
        val base = 'z' - 'a' + 1
        val name = StringBuilder("f")
        var currentCount = attributeNameCount
        do {
            val remainder = currentCount % base
            currentCount /= base
            name.insert(1, 'a' + remainder)
        } while (currentCount > 0)

        attributeNameCount += 1
        return name.toString()
    }

    private fun <T> Attribute<T>.filterOperator(op: String, value: T) = nextAttributeName().let { attributeName ->
        FilterExpression(
            "#$attributeName $op :$attributeName",
            mapOf("#$attributeName" to name),
            mapOf(":$attributeName" to asValue(value))
        )
    }

    private fun <T> Attribute<T>.filterOperator(op: String, other: Attribute<T>): FilterExpression {
        val attributeName1 = nextAttributeName()
        val attributeName2 = nextAttributeName()
        return FilterExpression(
            "#$attributeName1 $op #$attributeName2",
            mapOf("#$attributeName1" to name, "#$attributeName2" to other.name),
            emptyMap()
        )
    }

    infix fun <T> Attribute<T>.eq(value: T) = filterOperator("=", value)
    infix fun <T> Attribute<T>.eq(other: Attribute<T>) = filterOperator("=", other)
    infix fun <T> Attribute<T>.ne(value: T) = filterOperator("<>", value)
    infix fun <T> Attribute<T>.ne(other: Attribute<T>) = filterOperator("<>", other)
    infix fun <T> Attribute<T>.lt(value: T) = filterOperator("<", value)
    infix fun <T> Attribute<T>.lt(other: Attribute<T>) = filterOperator("<", other)
    infix fun <T> Attribute<T>.le(value: T) = filterOperator("<=", value)
    infix fun <T> Attribute<T>.le(other: Attribute<T>) = filterOperator("<=", other)
    infix fun <T> Attribute<T>.gt(value: T) = filterOperator(">", value)
    infix fun <T> Attribute<T>.gt(other: Attribute<T>) = filterOperator(">", other)
    infix fun <T> Attribute<T>.ge(value: T) = filterOperator(">=", value)
    infix fun <T> Attribute<T>.ge(other: Attribute<T>) = filterOperator(">=", other)

    fun <T> Attribute<T>.between(value1: T, value2: T) = nextAttributeName().let { attributeName ->
        FilterExpression(
            "#$attributeName BETWEEN :${attributeName}1 AND :${attributeName}2",
            mapOf("#$attributeName" to name),
            mapOf(":${attributeName}1" to asValue(value1), ":${attributeName}2" to asValue(value2))
        )
    }

    infix fun <T> Attribute<T>.isIn(values: Iterable<T>): FilterExpression {
        val attributeName = nextAttributeName()
        val attributeValues = mutableMapOf<String, AttributeValue>()
        val expression = StringBuilder("#$attributeName IN (")
        values.iterator().withIndex().forEach { (index, value) ->
            val valueName = ":$attributeName$index"
            attributeValues[valueName] = asValue(value)
            if (index > 0) {
                expression.append(',')
            }
            expression.append(valueName)
        }
        expression.append(')')

        require(attributeValues.isNotEmpty()) { "IN operator requires at least one element" }
        return FilterExpression(expression.toString(), mapOf("#$attributeName" to name), attributeValues)
    }

    fun attributeExists(attr: Attribute<*>): FilterExpression = nextAttributeName().let { attributeName ->
        FilterExpression(
            "attribute_exists(#$attributeName)",
            mapOf("#$attributeName" to attr.name),
            emptyMap()
        )
    }

    fun attributeNotExists(attr: Attribute<*>): FilterExpression = nextAttributeName().let { attributeName ->
        FilterExpression(
            "attribute_not_exists(#$attributeName)",
            mapOf("#$attributeName" to attr.name),
            emptyMap()
        )
    }

    infix fun <T> Attribute<T>.beginsWith(value: T) = nextAttributeName().let { attributeName ->
        FilterExpression(
            "begins_with(#$attributeName,:$attributeName)",
            mapOf("#$attributeName" to name),
            mapOf(":$attributeName" to asValue(value))
        )
    }

    infix fun <T> Attribute<T>.contains(value: T) = nextAttributeName().let { attributeName ->
        FilterExpression(
            "contains(#$attributeName,:$attributeName)",
            mapOf("#$attributeName" to name),
            mapOf(":$attributeName" to asValue(value))
        )
    }

    infix fun FilterExpression?.and(other: FilterExpression?): FilterExpression? = when {
        this == null -> other
        other == null -> this
        else -> FilterExpression(
            "($expression AND ${other.expression})",
            attributeNames + other.attributeNames,
            attributeValues + other.attributeValues
        )
    }

    infix fun FilterExpression?.or(other: FilterExpression?): FilterExpression? = when {
        this == null -> other
        other == null -> this
        else -> FilterExpression(
            "($expression OR ${other.expression})",
            attributeNames + other.attributeNames,
            attributeValues + other.attributeValues
        )
    }

    fun not(expr: FilterExpression?): FilterExpression? = expr?.let {
        FilterExpression(
            "(NOT ${it.expression})",
            it.attributeNames,
            it.attributeValues
        )
    }
}

class DynamoDbScanBuilder {

    internal class DynamoDbScan(
        val filterExpression: String?,
        val expressionAttributeNames: TokensToNames?,
        val expressionAttributeValues: TokensToValues?,
    )

    private var filterExpression: FilterExpression? = null

    fun filterExpression(block: FilterExpressionBuilder.() -> FilterExpression?) {
        filterExpression = block(FilterExpressionBuilder())
    }

    internal fun build() = DynamoDbScan(
        filterExpression = filterExpression?.expression,
        expressionAttributeNames = filterExpression?.attributeNames,
        expressionAttributeValues = filterExpression?.attributeValues
    )
}

class DynamoDbQueryBuilder<HashKey : Any, SortKey : Any>(private val schema: DynamoDbTableMapperSchema<*, HashKey, SortKey>) {

    internal class DynamoDbQuery(
        val keyConditionExpression: String?,
        val filterExpression: String?,
        val expressionAttributeNames: TokensToNames?,
        val expressionAttributeValues: TokensToValues?
    )

    private var keyCondition: KeyCondition<HashKey, SortKey>? = null
    private var filterExpression: FilterExpression? = null

    fun keyCondition(block: KeyConditionBuilder<HashKey, SortKey>.() -> CombinedKeyCondition<HashKey, SortKey>) {
        keyCondition = block(KeyConditionBuilder(schema))
    }

    fun filterExpression(block: FilterExpressionBuilder.() -> FilterExpression?) {
        filterExpression = block(FilterExpressionBuilder())
    }

    private fun <K, V> union(map1: Map<K, V>?, map2: Map<K, V>?) = when {
        map1 == null -> map2
        map2 == null -> map1
        else -> map1 + map2
    }

    internal fun build() = DynamoDbQuery(
        keyConditionExpression = keyCondition?.expression,
        filterExpression = filterExpression?.expression,
        expressionAttributeNames = union(
            keyCondition?.attributeNames,
            filterExpression?.attributeNames
        ),
        expressionAttributeValues = union(
            keyCondition?.attributeValues,
            filterExpression?.attributeValues
        )
    )
}

fun <Document : Any, HashKey : Any, SortKey : Any> DynamoDbIndexMapper<Document, HashKey, SortKey>.scan(
    PageSize: Int? = null,
    ConsistentRead: Boolean? = null,
    block: DynamoDbScanBuilder.() -> Unit
): Sequence<Document> {
    val scan = DynamoDbScanBuilder().apply(block).build()
    return scan(
        FilterExpression = scan.filterExpression,
        ExpressionAttributeNames = scan.expressionAttributeNames,
        ExpressionAttributeValues = scan.expressionAttributeValues,
        PageSize = PageSize,
        ConsistentRead = ConsistentRead
    )
}

fun <Document : Any, HashKey : Any, SortKey : Any> DynamoDbIndexMapper<Document, HashKey, SortKey>.scanPage(
    ExclusiveStartKey: Key? = null,
    Limit: Int? = null,
    ConsistentRead: Boolean? = null,
    block: DynamoDbScanBuilder.() -> Unit
): DynamoDbPage<Document> {
    val scan = DynamoDbScanBuilder().apply(block).build()
    return scanPage(
        FilterExpression = scan.filterExpression,
        ExpressionAttributeNames = scan.expressionAttributeNames,
        ExpressionAttributeValues = scan.expressionAttributeValues,
        ExclusiveStartKey = ExclusiveStartKey,
        Limit = Limit,
        ConsistentRead = ConsistentRead
    )
}

fun <Document : Any, HashKey : Any, SortKey : Any> DynamoDbIndexMapper<Document, HashKey, SortKey>.query(
    ScanIndexForward: Boolean = true,
    PageSize: Int? = null,
    ConsistentRead: Boolean? = null,
    block: DynamoDbQueryBuilder<HashKey, SortKey>.() -> Unit
): Sequence<Document> {
    val query = DynamoDbQueryBuilder(schema).apply(block).build()
    return query(
        KeyConditionExpression = query.keyConditionExpression,
        FilterExpression = query.filterExpression,
        ExpressionAttributeNames = query.expressionAttributeNames,
        ExpressionAttributeValues = query.expressionAttributeValues,
        ScanIndexForward = ScanIndexForward,
        PageSize = PageSize,
        ConsistentRead = ConsistentRead
    )
}

fun <Document : Any, HashKey : Any, SortKey : Any> DynamoDbIndexMapper<Document, HashKey, SortKey>.queryPage(
    ScanIndexForward: Boolean = true,
    Limit: Int? = null,
    ConsistentRead: Boolean? = null,
    ExclusiveStartKey: Key? = null,
    block: DynamoDbQueryBuilder<HashKey, SortKey>.() -> Unit
): DynamoDbPage<Document> {
    val query = DynamoDbQueryBuilder(schema).apply(block).build()
    return queryPage(
        KeyConditionExpression = query.keyConditionExpression,
        FilterExpression = query.filterExpression,
        ExpressionAttributeNames = query.expressionAttributeNames,
        ExpressionAttributeValues = query.expressionAttributeValues,
        ExclusiveStartKey = ExclusiveStartKey,
        ScanIndexForward = ScanIndexForward,
        Limit = Limit,
        ConsistentRead = ConsistentRead
    )
}

fun <Document : Any, HashKey : Any, SortKey : Any> DynamoDbIndexMapper<Document, HashKey, SortKey>.count(
    ConsistentRead: Boolean? = null,
    block: DynamoDbQueryBuilder<HashKey, SortKey>.() -> Unit
): Int {
    val query = DynamoDbQueryBuilder(schema).apply(block).build()
    return count(
        KeyConditionExpression = query.keyConditionExpression,
        FilterExpression = query.filterExpression,
        ExpressionAttributeNames = query.expressionAttributeNames,
        ExpressionAttributeValues = query.expressionAttributeValues,
        ConsistentRead = ConsistentRead
    )
}
