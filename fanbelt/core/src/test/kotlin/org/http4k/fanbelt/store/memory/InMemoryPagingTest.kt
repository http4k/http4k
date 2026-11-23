package org.http4k.fanbelt.store.memory

import org.http4k.fanbelt.store.PagingContract

class InMemoryPagingTest : PagingContract() {
    override val storage = InMemoryDocumentStorage()
}
