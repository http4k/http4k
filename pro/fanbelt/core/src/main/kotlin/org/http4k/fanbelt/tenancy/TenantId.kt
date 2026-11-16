/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.JobTopic

class TenantId private constructor(value: String) : StringValue(value) {
    operator fun invoke(topic: Topic) = Topic.of(if (topic is JobTopic) "$topic/$value" else "$value/$topic")

    companion object : StringValueFactory<TenantId>(::TenantId, "[A-Za-z0-9_-]{1,100}".regex) {
        internal fun ofJob(topic: Topic) = of(topic.value.substringAfterLast('/'))
    }
}
