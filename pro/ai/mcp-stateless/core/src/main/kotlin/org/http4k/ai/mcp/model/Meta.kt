/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject

data class Meta(val node: MoshiObject = MoshiObject()) {
    operator fun get(key: String): MoshiNode? = node[key]

    companion object {
        val default = Meta()

        operator fun invoke(vararg modifiers: (Meta) -> Meta): Meta =
            modifiers.fold(Meta()) { m, f -> f(m) }
    }
}

open class MetaField<T : Any>(val key: String)
