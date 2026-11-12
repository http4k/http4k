/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.fanbelt.infrastructure.Lifecycle

interface Bus : Lifecycle {
    fun publish(topic: Topic) = publish(setOf(topic))
    fun publish(topics: Set<Topic>)
    fun subscribe(topics: Set<Topic>, onChange: () -> Unit): Subscription
}
