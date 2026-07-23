package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

/**
 * The desired TTL configuration passed to UpdateTimeToLive, and echoed back in its response.
 * AttributeName is required even when disabling — it names the attribute TTL applies to.
 */
@JsonSerializable
data class TimeToLiveSpecification(
    val Enabled: Boolean,
    val AttributeName: AttributeName
)
