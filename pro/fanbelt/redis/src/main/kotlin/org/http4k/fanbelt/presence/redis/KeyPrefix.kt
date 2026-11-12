/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.redis

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class KeyPrefix private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<KeyPrefix>(::KeyPrefix) {
        val default = of("fanbelt:presence")
    }
}
