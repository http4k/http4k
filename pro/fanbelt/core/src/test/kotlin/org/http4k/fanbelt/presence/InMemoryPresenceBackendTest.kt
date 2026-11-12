/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend

class InMemoryPresenceBackendTest : PresenceBackendContract() {
    override fun backend() = InMemoryPresenceBackend(clock)
}
