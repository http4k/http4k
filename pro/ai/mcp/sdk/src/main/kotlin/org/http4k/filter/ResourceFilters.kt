package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.ResourceFilter
import org.http4k.ai.mcp.model.ResourceName

object ResourceFilters {
    fun OpenTelemetryTracing(resourceName: ResourceName) = ResourceFilter { next ->
        {
            Span.current().apply {
                setAttribute("gen_ai.resource.name", resourceName.value)
                setAttribute("gen_ai.operation.name", "read_resource")
            }
            next(it)
        }
    }
}
