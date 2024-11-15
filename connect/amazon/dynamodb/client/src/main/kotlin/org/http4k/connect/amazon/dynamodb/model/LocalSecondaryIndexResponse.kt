package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class LocalSecondaryIndexResponse(
    val IndexArn: ARN? = null,
    val IndexName: String? = null,
    val IndexSizeBytes: Long? = null,
    val ItemCount: Long? = null,
    val KeySchema: List<KeySchema>? = null,
    val Projection: Projection? = null
)
