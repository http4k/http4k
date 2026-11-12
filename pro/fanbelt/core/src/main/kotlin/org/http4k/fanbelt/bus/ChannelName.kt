/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class ChannelName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ChannelName>(::ChannelName, "[a-z_][a-z0-9_]{0,62}".regex) {
        val default = of("fanbelt")
    }
}
