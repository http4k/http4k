/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import org.http4k.ai.model.ToolName
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgUiTool(
    val name: ToolName,
    val description: String,
    val parameters: MoshiNode
)
