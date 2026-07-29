/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.format.stateless

import org.http4k.ai.mcp.stateless.model.Elicitation
import org.http4k.ai.mcp.stateless.model.ElicitationContentLensSpec
import org.http4k.ai.mcp.stateless.model.ElicitationModel
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.format.ConfigurableMoshi

inline fun <reified T : ElicitationModel> Elicitation.auto(t: T, json: ConfigurableMoshi = McpJson) =
    ElicitationContentLensSpec(t, json)
