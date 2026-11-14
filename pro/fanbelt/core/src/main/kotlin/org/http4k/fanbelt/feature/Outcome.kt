/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

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
