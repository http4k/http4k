package org.http4k.tracing

import org.http4k.events.MetadataEvent
import org.http4k.filter.ZipkinTraces

internal fun MetadataEvent.traces() = (metadata["traces"] as? ZipkinTraces)
