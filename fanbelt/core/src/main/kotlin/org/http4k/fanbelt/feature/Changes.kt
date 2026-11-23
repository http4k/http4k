package org.http4k.fanbelt.feature

import org.http4k.fanbelt.bus.Topic
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

/**
 * An accepted operation: it commits, then publishes its topics. A job's topic also records that job's key. The result
 * goes to the route's Feedback.
 */
@JsonSerializable
@PolymorphicLabel("changes")
@ConsistentCopyVisibility
data class Changes<out Result> internal constructor(val topics: Set<Topic>, val result: Result) : Outcome<Result>() {
    companion object {
        /**
         * Publishes these topics, with no result.
         */
        fun of(vararg topics: Topic) = Changes(topics.toSet(), Unit)

        /**
         * Publishes these topics, with no result.
         */
        fun of(topics: Set<Topic>) = Changes(topics, Unit)

        /**
         * Publishes these topics and gives the result, such as the id of what was created.
         */
        fun <Result> of(topics: Set<Topic>, result: Result) = Changes(topics, result)

        /**
         * Accepts without publishing, for an operation that changed nothing anyone reads.
         */
        fun none() = Changes(emptySet(), Unit)
    }
}
