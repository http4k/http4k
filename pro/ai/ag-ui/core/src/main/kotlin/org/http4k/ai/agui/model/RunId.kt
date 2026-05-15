/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.security.SecureRandom
import java.util.Random
import java.util.UUID

class RunId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<RunId>(::RunId) {
        fun random(random: Random = SecureRandom()) = of(UUID(random.nextLong(), random.nextLong()).toString())
    }
}
