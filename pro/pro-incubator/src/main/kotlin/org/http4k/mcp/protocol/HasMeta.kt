package org.http4k.mcp.protocol

import org.http4k.mcp.model.Meta

sealed interface HasMeta {
    val _meta: Meta get() = default

    companion object {
        val default = emptyMap<String, Any>()
    }
}
