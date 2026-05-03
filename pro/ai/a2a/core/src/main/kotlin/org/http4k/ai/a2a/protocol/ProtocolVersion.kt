/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol

import dev.forkhandles.values.ComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ProtocolVersion private constructor(value: String) : StringValue(value), ComparableValue<ProtocolVersion, String> {
    companion object : NonBlankStringValueFactory<ProtocolVersion>(::ProtocolVersion) {
        val `1_0_0` = of("1.0.0")

        val PUBLISHED = setOf(`1_0_0`)

        val LATEST_VERSION = PUBLISHED.max()
    }
}
