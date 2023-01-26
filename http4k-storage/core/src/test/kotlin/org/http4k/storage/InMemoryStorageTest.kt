package org.http4k.storage

class InMemoryStorageTest : StorageContract() {
    override val storage = Storage.InMemory<AnEntity>()
}
