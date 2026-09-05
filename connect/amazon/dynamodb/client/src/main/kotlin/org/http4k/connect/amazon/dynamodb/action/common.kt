package org.http4k.connect.amazon.dynamodb.action

import com.squareup.moshi.Json
import org.http4k.connect.amazon.dynamodb.model.ConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.ItemCollectionMetrics
import org.http4k.connect.amazon.dynamodb.model.ItemResult
import org.http4k.connect.amazon.dynamodb.model.TableDescription
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TableDescriptionResponse(
    val TableDescription: TableDescription
)

@JsonSerializable
data class ModifiedItem(
    val Attributes: ItemResult? = null,
    val ConsumedCapacity: ConsumedCapacity? = null,
    val ItemCollectionMetrics: ItemCollectionMetrics? = null
)

/**
 * The body of the error DynamoDB returns for a failed conditional write. Item holds the record which
 * blocked the write, and is populated only when the request set ReturnValuesOnConditionCheckFailure
 * to ALL_OLD.
 */
@JsonSerializable
data class ConditionalCheckFailed(
    val __type: String,
    /**
     * DynamoDB Local spells this `Message` - see
     * [org.http4k.connect.amazon.dynamodb.ConditionalCheckFailedAdapterFactory].
     */
    @Json(name = "message") val Message: String,
    val Item: ItemResult? = null
)

/**
 * The body of the error DynamoDB returns for a cancelled transaction: one reason per member of the
 * request, in order, with Code `None` for each member that passed.
 */
@JsonSerializable
data class TransactionCanceled(
    val __type: String,
    val Message: String,
    val CancellationReasons: List<CancellationReason>
)

@JsonSerializable
data class CancellationReason(
    val Code: String,
    val Message: String? = null,
    val Item: ItemResult? = null
)
