package org.http4k.lens

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.events.plus
import org.http4k.lens.ParamMeta.StringParam

/**
 * Reads and writes the metadata attached to an Event, such as a tenant or a trace id.
 */
object EventMetadata : BiDiLensSpec<Event, String>(
    "metadata", StringParam,
    LensGet { name, event -> listOfNotNull((event as? MetadataEvent)?.metadata?.get(name)?.toString()) },
    LensSet { name, values, event -> values.fold(event.withoutMetadata(name)) { tagged, value -> tagged + (name to value) } }
)

private fun Event.withoutMetadata(name: String): Event {
    if (this !is MetadataEvent) return this
    val remaining = metadata - name
    return if (remaining.isEmpty()) event else MetadataEvent(event, remaining)
}
