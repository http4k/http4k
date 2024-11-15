package org.http4k.connect.amazon.dynamodb.mapper

import org.http4k.connect.amazon.dynamodb.model.Key

data class DynamoDbPage<Document : Any>(
    val items: List<Document>,
    val lastEvaluatedKey: Key?
)
