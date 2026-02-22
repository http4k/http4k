package org.http4k.ai.mcp.server.capability

interface NamedServerCapability : ServerCapability {
    val name: String
}
