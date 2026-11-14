/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * The name of something a region shows, such as a stored state and its key, prefixed by its tenant on the bus. It
 * carries no state: publishing a topic only means "re-read it".
 */
class Topic private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Topic>(::Topic)
}
