/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.security.SecureRandom
import java.util.Random
import java.util.UUID

class PushNotificationConfigId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PushNotificationConfigId>(::PushNotificationConfigId) {
        /**
         * Generate a random MessageId
         */
        fun random(random: Random = SecureRandom()) = of(UUID(random.nextLong(), random.nextLong()).toString())
    }
}
