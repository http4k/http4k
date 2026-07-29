/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.util

import org.http4k.format.MoshiObject

object McpJson : ConfigurableMcpJson()

fun McpNodeType.errorCode(): Int? = (this as? MoshiObject)?.get("code")?.let { McpJson.integer(it).toInt() }
