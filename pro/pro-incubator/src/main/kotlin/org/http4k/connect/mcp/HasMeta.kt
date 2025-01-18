package org.http4k.connect.mcp

import org.http4k.connect.mcp.model.Meta

sealed interface HasMeta {
    val _meta: Meta get() = default

    companion object {
        val default = emptyMap<String, Any>()
    }
}
