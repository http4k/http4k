package org.http4k.connect.amazon.dynamodb.mapper

fun <Document : Any, HashKey : Any, SortKey : Any> DynamoDbTableMapper<Document, HashKey, SortKey>.update(
    hashKey: HashKey,
    sortKey: SortKey? = null,
    updateFn: (Document) -> Document
): Document? {
    val original = get(hashKey, sortKey) ?: return null
    val updated = updateFn(original)
    save(updated)
    return updated
}

operator fun <Document : Any, HashKey : Any> DynamoDbTableMapper<Document, HashKey, *>.get(
    vararg keys: HashKey
): Sequence<Document> = batchGet(keys.map { it to null })

operator fun <Document : Any, HashKey : Any> DynamoDbTableMapper<Document, HashKey, *>.get(
    keys: Collection<HashKey>
): Sequence<Document> = batchGet(keys.map { it to null })

operator fun <Document : Any> DynamoDbTableMapper<Document, *, *>.plusAssign(documents: Collection<Document>) =
    batchSave(documents)

operator fun <Document : Any> DynamoDbTableMapper<Document, *, *>.plusAssign(document: Document) =
    save(document)

operator fun <Document : Any> DynamoDbTableMapper<Document, *, *>.minusAssign(document: Document) =
    delete(document)

operator fun <Document : Any> DynamoDbTableMapper<Document, *, *>.minusAssign(documents: Collection<Document>) =
    batchDelete(documents)

fun <HashKey : Any> DynamoDbTableMapper<*, HashKey, *>.batchDelete(vararg hashKeys: HashKey) =
    batchDelete(hashKeys.toList())

fun <HashKey : Any> DynamoDbTableMapper<*, HashKey, *>.batchDelete(hashKeys: Collection<HashKey>) =
    batchDelete(hashKeys.map { it to null })
