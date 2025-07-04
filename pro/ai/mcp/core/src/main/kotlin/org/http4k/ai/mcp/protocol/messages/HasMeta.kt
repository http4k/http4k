package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta

sealed interface HasMeta {
    val _meta: Meta get() = Meta()
}
