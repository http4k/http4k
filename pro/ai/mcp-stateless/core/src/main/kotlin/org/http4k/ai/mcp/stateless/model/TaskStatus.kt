/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

enum class TaskStatus(val isTerminal: Boolean) {
    working(false),
    input_required(false),
    completed(true),
    cancelled(true),
    failed(true)
}
