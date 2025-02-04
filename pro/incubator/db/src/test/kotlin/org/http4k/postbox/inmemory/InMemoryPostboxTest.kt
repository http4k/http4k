package org.http4k.postbox.inmemory

import org.http4k.db.InMemoryTransactor
import org.http4k.postbox.PostboxContract

class InMemoryPostboxTest : PostboxContract() {
    override val postbox = InMemoryTransactor(InMemoryPostbox())
}

