package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

/**
 * The current TTL configuration of a table, as returned by DescribeTimeToLive. Both fields are optional
 * in the AWS response (mirroring the rest of this module's response models): AttributeName is present while
 * TTL is enabled or transitioning (ENABLING/DISABLING); once fully DISABLED the table reports no attribute.
 */
@JsonSerializable
data class TimeToLiveDescription(
    val TimeToLiveStatus: TimeToLiveStatus? = null,
    val AttributeName: AttributeName? = null
)
