/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory
import dev.forkhandles.values.between

class Priority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Priority>(::Priority, (0.0..1.0).between)
}
