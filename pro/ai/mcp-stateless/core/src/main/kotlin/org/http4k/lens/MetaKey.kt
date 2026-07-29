/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.MetaField
import org.http4k.ai.mcp.util.auto
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.lens.ParamMeta.ObjectParam

object MetaKey : BiDiLensSpec<Meta, MoshiNode>(
    "meta", ObjectParam,
    LensGet { name, target -> listOfNotNull(target[name]) },
    LensSet { name, values, target ->
        values.fold(target) { acc, next ->
            Meta(MoshiObject((acc.node.attributes + (name to next)).toMutableMap()))
        }
    }
)

inline fun <reified T : Any> MetaKey.progressToken() = auto<T>(MetaField("progressToken"))
fun MetaKey.traceParent() = auto<String>(MetaField("traceparent"))
fun MetaKey.traceState() = auto<String>(MetaField("tracestate"))
fun MetaKey.baggage() = auto<String>(MetaField("baggage"))
