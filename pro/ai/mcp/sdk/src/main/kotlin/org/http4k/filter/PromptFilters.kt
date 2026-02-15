package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.PromptFilter
import org.http4k.ai.mcp.model.PromptName

object PromptFilters {
    fun OpenTelemetryTracing(promptName: PromptName) = PromptFilter { next ->
        {
            Span.current().apply {
                setAttribute("gen_ai.prompt.name", promptName.value)
                setAttribute("gen_ai.operation.name", "get_prompt")
            }
            next(it)
        }
    }
}
