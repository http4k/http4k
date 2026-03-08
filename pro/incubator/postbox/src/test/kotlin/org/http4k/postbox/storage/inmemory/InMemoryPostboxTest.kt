package org.http4k.postbox.storage.inmemory

import dev.forkhandles.tx.mem.InMemoryTransactor
import org.http4k.postbox.storage.PostboxContract

class InMemoryPostboxTest : PostboxContract() {
    val inMemoryPostbox = InMemoryPostbox(timeSource)
    override val postbox = InMemoryTransactor(inMemoryPostbox, { inMemoryPostbox })
}

