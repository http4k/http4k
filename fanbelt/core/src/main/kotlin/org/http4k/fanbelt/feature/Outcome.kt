package org.http4k.fanbelt.feature

import org.http4k.core.Status
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.input.FieldName
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

/**
 * How an operation ended: Changes, Rejected, Denied or Hidden.
 */
@JsonSerializable
@Polymorphic("type")
sealed class Outcome<out Result> {
    @JsonSerializable
    @PolymorphicLabel("rejected")
    data class Rejected(val errors: Map<FieldName, String>) : Outcome<Nothing>(), Validated<Nothing>, Access<Nothing> {
        constructor(field: FieldName, message: String) : this(mapOf(field to message))
    }

    @JsonSerializable
    @PolymorphicLabel("denied")
    data object Denied : Outcome<Nothing>(), Access<Nothing>, Refusal

    @JsonSerializable
    @PolymorphicLabel("hidden")
    data object Hidden : Outcome<Nothing>(), Access<Nothing>, Refusal
}

/**
 * An accepted operation, and the topics it changed.
 */
@JsonSerializable
@PolymorphicLabel("changes")
@ConsistentCopyVisibility
data class Changes<out Result> internal constructor(val topics: Set<Topic>, val result: Result) : Outcome<Result>() {
    companion object {
        /**
         * Publishes these topics.
         */
        fun of(vararg topics: Topic) = Changes(topics.toSet(), Unit)

        /**
         * Publishes a set of topics.
         */
        fun of(topics: Set<Topic>) = Changes(topics, Unit)

        /**
         * Publishes these topics, with a result for the route's Feedback, such as the id of what was created.
         */
        fun <Result> of(topics: Set<Topic>, result: Result) = Changes(topics, result)

        /**
         * Publishes nothing, for an operation that changed nothing anyone reads.
         */
        fun none() = Changes(emptySet(), Unit)
    }
}

/**
 * Why a rule refused: Hidden or Denied.
 */
sealed interface Refusal

val Refusal.status: Status
    get() = when (this) {
        Denied -> FORBIDDEN
        Hidden -> NOT_FOUND
    }
