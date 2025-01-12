package org.http4k.connect.mcp

import com.fasterxml.jackson.databind.JsonNode

data class Tool(val name: String, val description: String, val schema: JsonNode)

