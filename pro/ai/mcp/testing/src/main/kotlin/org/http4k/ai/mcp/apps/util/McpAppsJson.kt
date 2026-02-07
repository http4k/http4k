package org.http4k.ai.mcp.apps.util

import com.squareup.moshi.JsonAdapter
import org.http4k.ai.mcp.util.ConfigurableMcpJson
import se.ansman.kotshi.KotshiJsonAdapterFactory

object McpAppsJson : ConfigurableMcpJson(McpAppsJsonFactory)

@KotshiJsonAdapterFactory
object McpAppsJsonFactory : JsonAdapter.Factory by KotshiMcpAppsJsonFactory
