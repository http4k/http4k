/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.lens.BiDiLens
import org.http4k.lens.LensInjectorExtractor
import org.http4k.lens.Meta

class McpCapabilityLens<IN : Any, OUT>(
    meta: Meta,
    get: (IN) -> OUT,
    private val set: (OUT, IN) -> IN,
    private val toSchema: (McpCapabilityLens<IN, *>) -> McpNodeType
) : LensInjectorExtractor<IN, OUT>, BiDiLens<IN, OUT>(meta, get, set) {

    @Suppress("UNCHECKED_CAST")
    override fun <R : IN> invoke(value: OUT, target: R): R = set(value, target) as R

    fun toSchema() = toSchema(this)
}
