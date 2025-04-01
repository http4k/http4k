package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Meta(val progress: ProgressToken? = null) {
    companion object {
        val default = Meta()
    }
}
