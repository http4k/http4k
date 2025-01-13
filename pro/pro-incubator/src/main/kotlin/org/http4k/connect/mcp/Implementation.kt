package org.http4k.connect.mcp

import org.http4k.connect.mcp.ProtocolVersion.Companion.LATEST_VERSION

data class Implementation(val name: String, val version: ProtocolVersion = LATEST_VERSION)
