package org.http4k.connect.amazon.eventbridge.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.eventbridge.EventBridgeAction
import org.http4k.connect.amazon.eventbridge.model.EventBus
import org.http4k.connect.amazon.model.EventBusName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListEventBuses(
    val NamePrefix: EventBusName? = null,
    val Limit: Int? = 100,
    val NextToken: String? = null
) : EventBridgeAction<EventBuses>(EventBuses::class)

@JsonSerializable
data class EventBuses(val EventBuses: List<EventBus>, val NextToken: String?)
