package org.http4k.connect.amazon.eventbridge.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.eventbridge.EventBridgeAction
import org.http4k.connect.amazon.eventbridge.EventBridgeMoshi
import org.http4k.connect.amazon.eventbridge.model.Event
import org.http4k.connect.amazon.model.EndpointId
import org.http4k.connect.amazon.model.EventId
import se.ansman.kotshi.JsonSerializable


@Http4kConnectAction
@JsonSerializable
data class PutEvents(
    val Entries: List<Event>,
    val EndpointId: EndpointId? = null
) : EventBridgeAction<EventResults>(EventResults::class, EventBridgeMoshi)

@JsonSerializable
data class EventResult(
    val EventId: EventId? = null,
    val ErrorCode: String? = null,
    val ErrorMessage: String? = null
)

@JsonSerializable
data class EventResults(val Entries: List<EventResult>, val FailedEntryCount: Int)
