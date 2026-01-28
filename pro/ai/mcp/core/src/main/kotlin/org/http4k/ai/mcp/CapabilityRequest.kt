package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.Meta

interface CapabilityRequest {
    val meta: Meta?
}
