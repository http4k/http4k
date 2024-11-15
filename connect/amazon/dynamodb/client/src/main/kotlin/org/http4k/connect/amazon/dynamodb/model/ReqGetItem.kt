package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@ExposedCopyVisibility
data class ReqGetItem internal constructor(
    val Keys: List<Item>,
    val ProjectionExpression: String? = null,
    val ExpressionAttributeNames: TokensToNames? = null,
    val ConsistentRead: Boolean? = null
) {
    companion object {
        fun Get(
            Keys: List<Item>,
            ProjectionExpression: String? = null,
            ExpressionAttributeNames: TokensToNames? = null,
            ConsistentRead: Boolean? = null
        ) = ReqGetItem(Keys, ProjectionExpression, ExpressionAttributeNames, ConsistentRead)
    }
}
