/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.CacheScope
import org.http4k.ai.mcp.model.TtlMs

interface CacheableResult {
    val ttlMs: TtlMs
    val cacheScope: CacheScope
}
