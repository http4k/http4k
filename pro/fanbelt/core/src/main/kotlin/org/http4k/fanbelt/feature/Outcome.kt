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
 * How an operation ended: `Changes` (accepted, its topics published after commit, with an optional result), `Rejected`
 * (errors on fields, shown to this viewer only), `Denied` (not allowed, a 403) or `Hidden` (as far as the actor knows
 * it doesn't exist, a 404, which only a rule answers). A refusal rolls back everything the operation wrote.
 */
@JsonSerializable
@Polymorphic("type")
sealed class Outcome<out Result> {
    @JsonSerializable
    @PolymorphicLabel("Rejected")
    data class Rejected(val errors: Map<FieldName, String>) : Outcome<Nothing>(), Validated<Nothing>, Access<Nothing> {
        constructor(field: FieldName, message: String) : this(mapOf(field to message))
    }

    @JsonSerializable
    @PolymorphicLabel("Denied")
    data object Denied : Outcome<Nothing>(), Access<Nothing>, Refusal

    @JsonSerializable
    @PolymorphicLabel("Hidden")
    data object Hidden : Outcome<Nothing>(), Access<Nothing>, Refusal
}
