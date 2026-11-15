/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.core.Status
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.events.Event
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden

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

data class OperationRefused(val operation: String, val rule: String, val refusal: Refusal) : Event
