package org.http4k.postbox

import org.http4k.db.InMemoryTransactor

class InMemoryPostboxTest : PostboxContract() {
    override val postbox = InMemoryTransactor(InMemoryPostbox())
}

