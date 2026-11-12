/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens.patched

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.events.plus
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.StringParam

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
