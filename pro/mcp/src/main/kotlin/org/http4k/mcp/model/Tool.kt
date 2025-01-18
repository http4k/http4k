package org.http4k.mcp.model

data class Tool<T>(val name: String, val description: String, val example: T)
