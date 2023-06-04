package org.http4k.tracing

import org.http4k.events.MetadataEvent

data class EventNode(val event: MetadataEvent, val children: List<EventNode>)
