package org.http4k.connect.amazon.eventbridge.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.eventbridge.EventBridgeAction
import org.http4k.connect.amazon.model.EventBusName
import org.http4k.connect.amazon.model.EventSourceName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateEventBus(
    val Name: EventBusName,
    val EventSourceName: EventSourceName? = null,
    val Tags: List<Tag>? = null
) : EventBridgeAction<CreatedEventBus>(CreatedEventBus::class)

@JsonSerializable
data class CreatedEventBus(val EventBusArn: ARN)
