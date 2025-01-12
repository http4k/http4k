package org.http4k.connect.mcp

sealed interface HasMeta {
    val _meta: Meta get() = default

    companion object {
        val default = emptyMap<String, Any>()
    }
}
