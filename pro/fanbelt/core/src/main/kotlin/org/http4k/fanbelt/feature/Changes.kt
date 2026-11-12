/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.bus.Topic
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("Changes")
@ConsistentCopyVisibility
data class Changes<out Result> internal constructor(val topics: Set<Topic>, val result: Result) : Outcome<Result>() {
    companion object {
        fun of(vararg topics: Topic) = Changes(topics.toSet(), Unit)

        fun of(topics: Set<Topic>) = Changes(topics, Unit)

        fun <Result> of(topics: Set<Topic>, result: Result) = Changes(topics, result)

        fun none() = Changes(emptySet(), Unit)
    }
}
