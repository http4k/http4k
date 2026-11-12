/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.Lifecycle
import kotlin.reflect.KClass

interface Presence : Lifecycle {
    fun present(topics: Map<Topic, KClass<out Any>>): Map<Topic, Map<PresenceMember, Any?>>

    fun join(topic: Topic, member: PresenceMember): Subscription

    fun set(topic: Topic, member: PresenceMember, value: Any)
}
