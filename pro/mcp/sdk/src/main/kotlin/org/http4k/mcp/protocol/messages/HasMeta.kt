package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Meta

sealed interface HasMeta {
    val _meta: Meta get() = Meta()

    companion object {
        val default = Meta()
    }
}
