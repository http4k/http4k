/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless

import org.http4k.ai.mcp.stateless.model.Meta

interface CapabilityRequest {
    val meta: Meta?
}
