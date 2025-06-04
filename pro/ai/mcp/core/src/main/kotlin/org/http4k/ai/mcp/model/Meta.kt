package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Meta(val progress: ProgressToken? = null) {
    companion object {
        val default = Meta()
    }
}
