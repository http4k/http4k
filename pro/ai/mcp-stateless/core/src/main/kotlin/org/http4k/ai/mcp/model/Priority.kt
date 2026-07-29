/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class Priority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Priority>(::Priority, _0_to_1)
}
