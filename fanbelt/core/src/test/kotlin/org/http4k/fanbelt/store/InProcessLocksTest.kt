package org.http4k.fanbelt.store

import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage

class InProcessLocksTest : LocksContract() {
    override val locks = InMemoryDocumentStorage().locks
    override val elsewhere = locks
}
