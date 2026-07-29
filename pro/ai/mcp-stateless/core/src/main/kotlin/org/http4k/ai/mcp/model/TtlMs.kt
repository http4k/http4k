/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory

class TtlMs private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<TtlMs>(::TtlMs)
}
