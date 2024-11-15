package org.http4k.connect.storage

import kotlin.io.path.createTempDirectory

class DiskStorageTest : StorageContract() {
    override val storage = Storage.Disk<AnEntity>(createTempDirectory().toFile())
}
