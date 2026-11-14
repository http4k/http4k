/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.events.Event

/**
 * How a rule refused an operation, as reported by `OperationRefused`: `Outcome.Hidden` (a 404) or `Outcome.Denied`
 * (a 403).
 */
sealed interface Refusal

data class OperationRefused(val operation: String, val rule: String, val refusal: Refusal) : Event
