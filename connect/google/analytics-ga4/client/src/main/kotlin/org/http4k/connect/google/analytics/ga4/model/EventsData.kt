package org.http4k.connect.google.analytics.ga4.model

import org.http4k.connect.google.analytics.model.ClientId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class EventsData(
    val client_id: ClientId,
    val events: List<EventData>
)

@JsonSerializable
data class EventData(
    val name: String,
    val params: Map<String, Any>
)
