/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class OtelSpanId private constructor(value: String) : StringValue(value) {
    val short get() = value.take(8)
    companion object : NonBlankStringValueFactory<OtelSpanId>(::OtelSpanId)
}
