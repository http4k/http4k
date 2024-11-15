package org.http4k.connect.amazon.eventbridge.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.eventbridge.EventBridgeAction
import org.http4k.connect.amazon.model.EventBusName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteEventBus(val Name: EventBusName) : EventBridgeAction<Unit>(Unit::class)
