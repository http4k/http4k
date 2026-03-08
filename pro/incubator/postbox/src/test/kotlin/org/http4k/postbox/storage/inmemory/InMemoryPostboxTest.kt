/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.inmemory

import dev.forkhandles.tx.mem.InMemoryTransactor
import org.http4k.postbox.storage.PostboxContract

class InMemoryPostboxTest : PostboxContract() {
    val inMemoryPostbox = InMemoryPostbox(timeSource)
    override val postbox = InMemoryTransactor(inMemoryPostbox, { inMemoryPostbox })
}

