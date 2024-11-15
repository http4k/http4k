package org.http4k.connect.storage

class InMemoryStorageTest : StorageContract() {
    override val storage = Storage.InMemory<AnEntity>()
}
