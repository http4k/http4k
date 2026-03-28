/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.orThrow
import dev.forkhandles.result4k.valueOrNull

typealias McpResult<T> = Result4k<T, McpError>

/**
 * Coerce result into expected type or throw
 */
inline fun <reified T> McpResult<*>.coerce(): T {
    val msg = { v: Any? -> "Expected Success<${T::class.simpleName}> but was $v" }
    return orThrow { AssertionError(msg(it)) } as? T ?: throw AssertionError(msg(valueOrNull()))
}
