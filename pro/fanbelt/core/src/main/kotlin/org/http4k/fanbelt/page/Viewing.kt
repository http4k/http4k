/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.fanbelt.tenancy.Scope

/**
 * How each browser's connection runs: `actor` names who is watching in traces, and `onConnect` runs when the stream
 * opens (to join a presence state, say), its result closed when the stream ends.
 */
class Viewing(
    val actor: (Request) -> Any = { },
    val onConnect: (Scope, Request) -> AutoCloseable = { _, _ -> AutoCloseable {} },
)
