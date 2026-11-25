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
 * How an operation ended: Changes, Rejected, Denied (403) or Hidden (404). Anything but Changes rolls back.
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

/**
 * Why a rule refused: Hidden or Denied.
 */
sealed interface Refusal

/**
 * How a refusal is answered over HTTP: Denied is a 403, Hidden a 404.
 */
val Refusal.status: Status
    get() = when (this) {
        Denied -> FORBIDDEN
        Hidden -> NOT_FOUND
    }
