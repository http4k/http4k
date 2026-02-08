@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.model.apps.McpAppMeta
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Meta(
    val progressToken: ProgressToken? = null,
    @JsonProperty("io.modelcontextprotocol/related-task")
    val relatedTask: RelatedTaskMetadata? = null,
    val ui: McpAppMeta? = null,
) {
    companion object {
        val default = Meta()
    }
}

