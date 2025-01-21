package org.http4k.mcp.model

data class Prompt(val name: String, val description: String?, val arguments: List<Argument> = emptyList()) {
    data class Argument(val name: String, val description: String? = null, val required: Boolean? = null)
}
