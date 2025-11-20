package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.TaskMeta

interface CapabilityRequest {
    val meta: Meta?
    val task: TaskMeta?
}
