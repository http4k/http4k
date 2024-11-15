package org.http4k.connect.amazon.firehose.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.firehose.FirehoseAction
import org.http4k.connect.amazon.model.DeliveryStreamName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteDeliveryStream(
    val DeliveryStreamName: DeliveryStreamName,
    val AllowForceDelete: Boolean? = false
) : FirehoseAction<Unit>(Unit::class)
