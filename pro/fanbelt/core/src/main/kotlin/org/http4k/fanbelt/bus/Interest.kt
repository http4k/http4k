/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * What a transport listens for: the part of a topic before its first '/', which is its tenant. A transport can
 * subscribe per interest so that a node hears only the tenants it is showing.
 */
class Interest private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Interest>(::Interest) {
        fun of(topic: Topic) = of(topic.value.substringBefore('/'))
    }
}
