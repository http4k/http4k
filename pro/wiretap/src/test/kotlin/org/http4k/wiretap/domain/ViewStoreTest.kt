package org.http4k.wiretap.domain

class InMemoryViewStoreTest : ViewStoreContract {
    override val store = ViewStore.InMemory()
}
