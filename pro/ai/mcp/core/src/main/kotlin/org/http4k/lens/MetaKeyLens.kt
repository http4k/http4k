/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens

import org.http4k.ai.mcp.model.Meta

class MetaKeyLens<FINAL>(
    private val getFn: (Meta) -> FINAL,
    private val setFn: (FINAL, Meta) -> Meta
) : LensInjectorExtractor<Meta, FINAL> {
    override fun invoke(target: Meta): FINAL = getFn(target)

    @Suppress("UNCHECKED_CAST")
    override fun <R : Meta> invoke(value: FINAL, target: R): R = setFn(value, target) as R
}
