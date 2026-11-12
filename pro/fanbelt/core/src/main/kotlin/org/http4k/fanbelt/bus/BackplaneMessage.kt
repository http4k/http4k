/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class BackplaneMessage private constructor(value: String) : StringValue(value) {
    internal val from get() = NodeId.parse(value.substringBefore(' '))
    val topic get() = Topic.of(value.substringAfter(' '))
    val interest get() = Interest.of(topic)

    companion object : NonBlankStringValueFactory<BackplaneMessage>(::BackplaneMessage) {
        internal fun of(from: NodeId, topic: Topic) = of("$from ${topic.value}")
    }
}
