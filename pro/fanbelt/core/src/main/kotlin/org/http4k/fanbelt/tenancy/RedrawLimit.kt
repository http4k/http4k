/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.minValue

class RedrawLimit private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<RedrawLimit>(::RedrawLimit, 1.minValue)
}
