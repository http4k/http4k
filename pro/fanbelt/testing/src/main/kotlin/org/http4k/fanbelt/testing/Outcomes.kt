/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome

fun <Result> Outcome<Result>.accepted(): Result = when (this) {
    is Changes -> result
    else -> error("expected the operation to be accepted, but it was $this")
}
