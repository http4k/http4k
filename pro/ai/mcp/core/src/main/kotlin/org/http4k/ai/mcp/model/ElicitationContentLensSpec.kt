/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.ElicitationResponse.Ok
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.ConfigurableMoshi
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam

class ElicitationContentLensSpec<OUT : ElicitationModel>(
    private val example: OUT,
    internal val json: ConfigurableMoshi = McpJson
) {
    fun toLens(name: String, description: String) = McpCapabilityLens(
        Meta(true, "elicitationResponse", ObjectParam, name, description, emptyMap()),
        { json.asA(json.asFormatString(it.content), example::class) },
        { out, target: Ok -> target.copy(content = json.asJsonObject(out)) },
        { example.toSchema() }
    )
}
