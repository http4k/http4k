package org.http4k.connect.amazon.eventbridge.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.model.EventBusName
import org.http4k.connect.amazon.model.Policy
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class EventBus(
    val Arn: ARN? = null,
    val Name: EventBusName? = null,
    val Policy: Policy? = null
)
