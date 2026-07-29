/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.connect.model.TimeToLive
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskMeta(val ttl: TimeToLive? = null)
