/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.apps.util

import com.squareup.moshi.JsonAdapter
import org.http4k.ai.mcp.util.ConfigurableMcpJson
import se.ansman.kotshi.KotshiJsonAdapterFactory

object McpAppsJson : ConfigurableMcpJson(McpAppsJsonFactory)

@KotshiJsonAdapterFactory
object McpAppsJsonFactory : JsonAdapter.Factory by KotshiMcpAppsJsonFactory
