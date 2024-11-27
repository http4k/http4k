package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class BillingModeSummary(
    val BillingMode: BillingMode? = null,
    val LastUpdateToPayPerRequestDateTime: Timestamp? = null
)
