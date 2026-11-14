/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Identifies something that can change, such as a stored state for a key. Publishing it means "re-read this".
 */
class Topic private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Topic>(::Topic)
}
