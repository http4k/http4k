/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens

import org.http4k.ai.mcp.model.Meta
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject

class MetaLensSpec<T : Any>(
    private val name: String,
    private val getFn: (MoshiNode) -> T,
    private val setFn: (T) -> MoshiNode
) {
    fun toLens() = MetaKeyLens(
        { it[name]?.let(getFn) },
        { value, target ->
            if (value != null) Meta(MoshiObject((target.node.attributes + (name to setFn(value))).toMutableMap()))
            else target
        }
    )
}
