package org.http4k.connect.amazon.dynamodb.mapper

import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.LocalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.Projection
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.format.autoDynamoLens
import org.http4k.lens.BiDiLens

sealed interface DynamoDbTableMapperSchema<Document: Any, HashKey, SortKey> {
    val hashKeyAttribute: Attribute<HashKey>
    val sortKeyAttribute: Attribute<SortKey>?
    val indexName: IndexName?
    val lens: BiDiLens<Item, Document>

    fun keySchema() = KeySchema.compound(hashKeyAttribute.name, sortKeyAttribute?.name)
    fun attributeDefinitions() = setOfNotNull(
        hashKeyAttribute.asAttributeDefinition(),
        sortKeyAttribute?.asAttributeDefinition()
    )

    data class Primary<Document: Any, HashKey, SortKey>(
        override val hashKeyAttribute: Attribute<HashKey>,
        override val sortKeyAttribute: Attribute<SortKey>?,
        override val lens: BiDiLens<Item, Document>
    ) : DynamoDbTableMapperSchema<Document, HashKey, SortKey> {
        companion object {
            inline operator fun <reified Document: Any, HashKey, SortKey> invoke(
                hashKeyAttribute: Attribute<HashKey>,
                sortKeyAttribute: Attribute<SortKey>? = null,
                lens: BiDiLens<Item, Document> = DynamoDbMoshi.autoDynamoLens()
            ) = Primary(hashKeyAttribute, sortKeyAttribute, lens)
        }

        override val indexName = null
    }

    sealed interface Secondary<Document: Any, HashKey, SortKey> : DynamoDbTableMapperSchema<Document, HashKey, SortKey>

    data class GlobalSecondary<Document: Any, HashKey, SortKey>(
        override val indexName: IndexName,
        override val hashKeyAttribute: Attribute<HashKey>,
        override val sortKeyAttribute: Attribute<SortKey>?,
        override val lens: BiDiLens<Item, Document>,
        val projection: Projection = Projection.all
    ) : Secondary<Document, HashKey, SortKey> {

        companion object {
            inline operator fun <reified Document: Any, HashKey, SortKey> invoke(
                indexName: IndexName,
                hashKeyAttribute: Attribute<HashKey>,
                sortKeyAttribute: Attribute<SortKey>? = null,
                lens: BiDiLens<Item, Document> = DynamoDbMoshi.autoDynamoLens(),
                projection: Projection = Projection.all
            ) = GlobalSecondary(indexName, hashKeyAttribute, sortKeyAttribute, lens, projection)
        }

        fun toIndex() = GlobalSecondaryIndex(
            IndexName = indexName,
            KeySchema = keySchema(),
            Projection = projection
        )
    }

    data class LocalSecondary<Document: Any, HashKey, SortKey>(
        override val indexName: IndexName,
        override val hashKeyAttribute: Attribute<HashKey>,
        override val sortKeyAttribute: Attribute<SortKey>?,
        override val lens: BiDiLens<Item, Document>,
        val projection: Projection = Projection.all
    ) : Secondary<Document, HashKey, SortKey> {
        companion object {
            inline operator fun <reified Document: Any, HashKey, SortKey> invoke(
                indexName: IndexName,
                hashKeyAttribute: Attribute<HashKey>,
                sortKeyAttribute: Attribute<SortKey>? = null,
                lens: BiDiLens<Item, Document> = DynamoDbMoshi.autoDynamoLens(),
                projection: Projection = Projection.all
            ) = LocalSecondary(indexName, hashKeyAttribute, sortKeyAttribute, lens, projection)
        }

        fun toIndex() = LocalSecondaryIndex(
            IndexName = indexName,
            KeySchema = keySchema(),
            Projection = projection
        )
    }
}

fun <HashKey, SortKey> DynamoDbTableMapperSchema<*, HashKey, SortKey>.key(hashKey: HashKey, sortKey: SortKey?): Key {
    return if (sortKeyAttribute == null || sortKey == null) {
        Item(hashKeyAttribute of hashKey)
    } else Item(
        hashKeyAttribute of hashKey,
        sortKeyAttribute!! of sortKey
    )
}
