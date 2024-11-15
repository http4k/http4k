package org.http4k.connect.amazon.firehose.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.DeliveryStreamType
import org.http4k.connect.amazon.firehose.FirehoseAction
import org.http4k.connect.amazon.model.DeliveryStreamName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListDeliveryStreams(
    val DeliveryStreamType: DeliveryStreamType? = null,
    val ExclusiveStartDeliveryStreamName: String? = null,
    val Limit: Int? = 10
) : FirehoseAction<DeliveryStreams>(DeliveryStreams::class)

@JsonSerializable
data class DeliveryStreams(
    val DeliveryStreamNames: List<DeliveryStreamName>,
    val HasMoreDeliveryStreams: Boolean
)
