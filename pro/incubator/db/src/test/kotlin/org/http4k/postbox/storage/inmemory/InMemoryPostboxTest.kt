package org.http4k.postbox.storage.inmemory

import org.http4k.db.InMemoryTransactor
import org.http4k.postbox.storage.PostboxContract

class InMemoryPostboxTest : PostboxContract() {
    override val postbox = InMemoryTransactor(InMemoryPostbox(timeSource))
}

